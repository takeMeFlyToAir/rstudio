/*
 * SpellChecker.java
 *
 * Copyright (C) 2009-19 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.spelling;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.Mutable;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.jsonrpc.RequestLog;
import org.rstudio.core.client.jsonrpc.RequestLogEntry;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.spelling.model.SpellCheckerResult;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchList;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class TypoSpellChecker
{
   private class TypoDictionaryRequest
   {
      public TypoDictionaryRequest(String language, boolean isCustom)
      {
         language_ = language;
         isCustom_ = isCustom;
      }
      
      public void send()
      {
         String path = GWT.getHostPageBaseURL() + "dictionaries/" + language_ + "/" + language_;

         final Mutable<String> aff = new Mutable<String>();
         final Mutable<String> dic = new Mutable<String>();
         final Command onReady = () -> {

            if (cancelled_ || aff.get() == null || dic.get() == null)
               return;

            typoLoader_.addCallback(() -> {
               TypoNative typo = new TypoNative(language_, aff.get(), dic.get(), null);
               if (!isCustom_)
               {
                  typoNative_ = typo;
                  loadedDict_ = language_;
                  typoLoaded_ = true;
               }
               else
                  customTypoNative_.put(language_, typo);

               aff.clear();
               dic.clear();
               alive_ = false;
            });
            
         };

         alive_ = true;
         makeRequest(path, "aff", (String response) -> {
            aff.set(response);
            onReady.execute();
         });

         makeRequest(path, "dic", (String response) -> {
            dic.set(response);
            onReady.execute();
         });

      }
      
      private void makeRequest(String path,
                               String suffix,
                               final CommandWithArg<String> callback)
      {
         String logName = language_ + "_" + suffix + "_request";
         final RequestLogEntry logEntry = RequestLog.log(logName, "");

         try
         {
            RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, path + "." + suffix);
            builder.sendRequest("", new RequestCallback()
            {
               @Override
               public void onResponseReceived(Request request, Response response)
               {
                  logEntry.logResponse(RequestLogEntry.ResponseType.Normal, response.getText());
                  callback.execute(response.getText());
               }

               @Override
               public void onError(Request request, Throwable throwable)
               {
                  logEntry.logResponse(RequestLogEntry.ResponseType.Error, throwable.getLocalizedMessage());
                  alive_ = false;
               }
            });
         }
         catch (RequestException e)
         {
            logEntry.logResponse(RequestLogEntry.ResponseType.Unknown, e.getLocalizedMessage());
            alive_ = false;
         }
      }
      
      
      public String getLanguage()
      {
         return language_;
      }
      
      public void cancel()
      {
         cancelled_ = true;
      }
      
      public boolean isAlive()
      {
         return alive_;
      }
      
      private boolean cancelled_;
      private boolean alive_;
      
      private final String language_;
      private final boolean isCustom_;
   }
   
   public interface Context
   {
      ArrayList<String> readDictionary();
      void writeDictionary(ArrayList<String> words);

      void invalidateAllWords();
      void invalidateMisspelledWords();
      void invalidateWord(String word);

      void releaseOnDismiss(HandlerRegistration handler);
   }

   interface Resources extends ClientBundle
   {
      @Source("./typo.min.js")
      TextResource typoJsCode();

      @Source("./domain_specific_words.csv")
      TextResource domainSpecificWords();
   }

   public TypoSpellChecker(Context context)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      // save reference to context and read its dictionary
      context_ = context;
      contextDictionary_ = context_.readDictionary();

      // subscribe to spelling prefs changes (invalidateAll on changes)
      ValueChangeHandler<Boolean> prefChangedHandler = (event) -> context_.invalidateAllWords();
      ValueChangeHandler<Boolean> realtimeChangedHandler = (event) -> loadDictionary();
      ValueChangeHandler<String> dictChangedHandler = (event) -> loadDictionary();
      userPrefs_.ignoreUppercaseWords().addValueChangeHandler(prefChangedHandler);
      userPrefs_.ignoreWordsWithNumbers().addValueChangeHandler(prefChangedHandler);
      userPrefs_.spellingDictionaryLanguage().addValueChangeHandler(dictChangedHandler);
      userPrefs_.realTimeSpellchecking().addValueChangeHandler(realtimeChangedHandler);

      // subscribe to user dictionary changes
      context_.releaseOnDismiss(userDictionary_.addListChangedHandler((ListChangedEvent event) ->
         {
            // detect whether this is the first delivery of the list
            // or if it is an update
            userDictionaryWords_ = event.getList();
            updateIgnoredWordsIndex();
         }
      ));
   }

   @Inject
   void initialize(SpellingService spellingService, WorkbenchListManager workbenchListManager, UserPrefs uiPrefs)
   {
      userDictionary_ = workbenchListManager.getUserDictionaryList();
      userPrefs_ = uiPrefs;

      if (!spellingWorkerInitialized_)
      {
         spellingWorkerInitialized_ = true;
         ExternalJavaScriptLoader.Callback loadSpellingWorker = () -> {
            spellingPrefetcherNative_ = new SpellingPrefetcherNative(RES.typoJsCode().getText());
         };
         new ExternalJavaScriptLoader(
            SpellingPrefetcherResources.INSTANCE.spellingprefetcherjs().getSafeUri().asString()
         ).addCallback(loadSpellingWorker);
      }

      if (domainSpecificWords_.isEmpty())
      {
         String[] words = RES.domainSpecificWords().getText().split("[\\r\\n]+");
         for (String w : words)
         {
            if (w.length() > 0)
               domainSpecificWords_.add(w.toLowerCase());
         }
      }
      loadDictionary();

      // if the user has custom dictionaries set, load those as well
      JsArrayString customDictionaries = uiPrefs.spellingCustomDictionaries().getValue();
      if (customDictionaries.length() > 0)
         loadCustomDictionaries(customDictionaries);
   }

   // Check the spelling of a single word, directly returning an
   // array of suggestions for corrections. The array is empty if the
   // word is deemed correct by the dictionary
   public boolean checkSpelling(String word)
   {
      return domainSpecificWords_.contains(word.toLowerCase()) ||
         allIgnoredWords_.contains(word) ||
         typoNative_.check(word) ||
         checkCustomDicts(word);
   }

   // go through all of the custom dictionaries and check the word
   private boolean checkCustomDicts(String word)
   {
      for (Map.Entry<String, TypoNative> pair : customTypoNative_.entrySet())
      {
         TypoNative dictNative = pair.getValue();
         if (dictNative != null)
         {
            if (dictNative.check(word))
               return true;
         }
      }
      return false;
   }

   public void checkSpelling(List<String> words, final ServerRequestCallback<SpellCheckerResult> callback)
   {
      // allocate results
      final SpellCheckerResult spellCheckerResult = new SpellCheckerResult();

      // if not checking any words, return
      if (words.isEmpty())
      {
         callback.onResponseReceived(spellCheckerResult);
         return;
      }

      for (String word : words)
      {
         if (isWordIgnored(word))
         {
            spellCheckerResult.getCorrect().add(word);
         }
         else
         {
            if (typoNative_.check(word))
            {
               spellCheckerResult.getCorrect().add(word);
            }
            else
            {
               spellCheckerResult.getIncorrect().add(word);
            }
         }
      }
      callback.onResponseReceived(spellCheckerResult);
   }

   public void addToUserDictionary(final String word)
   {
      userDictionary_.append(word);
      context_.invalidateWord(word);
   }

   public void addIgnoredWord(String word)
   {
      contextDictionary_.add(word);
      context_.writeDictionary(contextDictionary_);
      updateIgnoredWordsIndex();
      context_.invalidateWord(word);
   }

   public String[] suggestionList(String word)
   {
      if (typoNative_ == null)
      {
         return new String[0];
      }

      return typoNative_.suggest(word);
   }
   private boolean isWordIgnored(String word)
   {
      return (allIgnoredWords_.contains(word) ||
              ignoreUppercaseWord(word) ||
              ignoreWordWithNumbers(word));
   }

   private boolean ignoreUppercaseWord(String word)
   {
      if (!userPrefs_.ignoreUppercaseWords().getValue())
         return false;

      for (char c: word.toCharArray())
      {
         if(!Character.isUpperCase(c))
            return false;
      }
      return true;
   }

   private boolean ignoreWordWithNumbers(String word)
   {
      if (!userPrefs_.ignoreWordsWithNumbers().getValue())
         return false;

      for (char c: word.toCharArray())
      {
         if(Character.isDigit(c))
            return true;
      }
      return false;
   }

   private void updateIgnoredWordsIndex()
   {
      allIgnoredWords_.clear();
      allIgnoredWords_.addAll(userDictionaryWords_);
      allIgnoredWords_.addAll(contextDictionary_);
   }

   private void loadDictionary()
   {
      final String language = userPrefs_.spellingDictionaryLanguage().getValue();

      if (!userPrefs_.realTimeSpellchecking().getValue() || typoLoaded_ && loadedDict_.equals(language))
         return;

      // See canRealtimeSpellcheckDict comment, temporary stop gap
      // Return early if this dictionary is incompatible
      // with Typo.js. Final invariant check to ensure that we never try to
      // load a blacklisted dictionary.
      if (!canRealtimeSpellcheckDict(language))
         return;

      // check for an active request
      if (activeRequest_ != null && activeRequest_.isAlive())
      {
         // if we're already requesting this language's dictionary, bail
         if (StringUtil.equals(activeRequest_.getLanguage(), language))
            return;

         // otherwise, cancel that request and start a new one
         activeRequest_.cancel();
         activeRequest_ = null;
      }

      // create and send
      activeRequest_ = new TypoDictionaryRequest(language, false);
      activeRequest_.send();
   }

   private void loadCustomDictionaries(JsArrayString customDictionaries)
   {
      for (int i = 0; i < customDictionaries.length(); i++)
      {
         String dict = customDictionaries.get(i);
         if (!customTypoNative_.containsKey(dict))
         {
            customTypoNative_.put(dict, null);
            TypoDictionaryRequest req = new TypoDictionaryRequest(dict, true);
            req.send();
         }
      }
   }

   public void prefetchWords(ArrayList<String> words)
   {
      if (spellingWorkerInitialized_)
      {
         spellingPrefetcherNative_.prefetch(String.join(",", words), typoNative_);
      }
   }

   public boolean shouldCheckSpelling(DocDisplay dd, Range r)
   {
      String word = dd.getTextForRange(r);
      // Don't worry about pathologically long words
      if (r.getEnd().getColumn() - r.getStart().getColumn() > 250)
         return false;

      if (isWordIgnored(word))
         return false;

      // Don't spellcheck yaml
      Scope s = ((AceEditor)dd).getScopeAtPosition(r.getStart());
      if (s != null && s.isYaml())
         return false;

      // This will capture all braced text in a way that the
      // highlight rules don't and shouldn't.
      int start = r.getStart().getColumn();
      int end = start + word.length();
      String line = dd.getLine(r.getStart().getRow());
      Pattern p =  Pattern.create("\\{[^\\{\\}]*" + word + "[^\\{\\}]*\\}");
      Match m = p.match(line, 0);
      while (m != null)
      {
         // ensure that the match is the specific word we're looking
         // at to fix edge cases such as {asdf}asdf
         if (m.getIndex() < start &&
             (m.getIndex() + m.getValue().length()) > end)
            return false;

         m = m.nextMatch();
      }

      return true;
   }

   /*
      Stop gap function to prevent loading dictionaries that Typo.js has
      severe issues with. This is being tracked to be fixed in issue #6041 as
      soon as possible so this can then be removed.
    */
   private static String[] realtimeDictBlacklist = {"cs_CZ", "de_DE_neu", "lt_LT", "pt_BR", "it_IT"};
   public static boolean canRealtimeSpellcheckDict(String dict)
   {
      boolean exists = false;
      for (int i = 0; i < realtimeDictBlacklist.length; i++)
      {
         if (realtimeDictBlacklist[i].equals(dict))
         {
            exists = true;
            break;
         }
      }
      return !exists;
   }

   public static boolean isLoaded() { return typoLoaded_; }

   private final Context context_;
   private static final Resources RES = GWT.create(Resources.class);

   private static SpellingPrefetcherNative spellingPrefetcherNative_;
   private static boolean spellingWorkerInitialized_ = false;

   private static String loadedDict_;
   private static boolean typoLoaded_ = false;
   private static TypoNative typoNative_;
   private static HashMap<String, TypoNative> customTypoNative_ = new HashMap<>();
   private static TypoDictionaryRequest activeRequest_;

   private WorkbenchList userDictionary_;
   private ArrayList<String> userDictionaryWords_;
   private ArrayList<String> contextDictionary_;
   private final HashSet<String> allIgnoredWords_ = new HashSet<>();
   private final HashSet<String> domainSpecificWords_ = new HashSet<>();
   private final ExternalJavaScriptLoader typoLoader_ =
         new ExternalJavaScriptLoader(TypoResources.INSTANCE.typojs().getSafeUri().asString());

   private UserPrefs userPrefs_;
}

