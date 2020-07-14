/*
 * EditingTargetCodeExecution.java
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

package org.rstudio.studio.client.workbench.views.source.editors;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.common.mathjax.MathJaxUtil;
import org.rstudio.studio.client.rmarkdown.events.SendToChunkConsoleEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleExecutePendingInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.JobRunSelectionEvent;
import org.rstudio.studio.client.workbench.views.jobs.events.LauncherJobRunSelectionEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Mode.InsertChunkInfo;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.terminal.events.CreateNewTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.SendToTerminalEvent;

import com.google.inject.Inject;

public class EditingTargetCodeExecution
{
   public interface CodeExtractor
   {
      String extractCode(DocDisplay docDisplay, Range range);
   }
   
   public EditingTargetCodeExecution(DocDisplay display, String docId)
   {
      this(null, display, docId, new CodeExtractor() {
         @Override
         public String extractCode(DocDisplay docDisplay, Range range)
         {
            return docDisplay.getCode(range.getStart(), range.getEnd());
         }
      });
   }
   
   public EditingTargetCodeExecution(TextEditingTarget target,
                                     DocDisplay display,
                                     String docId,
                                     CodeExtractor codeExtractor)
   {
      target_ = target;
      docDisplay_ = display;
      codeExtractor_ = codeExtractor;
      docId_ = docId;
      inlineChunkExecutor_ = new EditingTargetInlineChunkExecution(
            display, docId);
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(EventBus events,
                   UserPrefs prefs,
                   Commands commands,
                   SourceServerOperations server)
   {
      events_ = events;
      prefs_ = prefs;
      commands_ = commands;
      server_ = server;
   }
   
   public void executeSelection(boolean consoleExecuteWhenNotFocused,
                                boolean moveCursorAfter)
   {
      executeSelectionMaybeNoFocus(consoleExecuteWhenNotFocused,
            moveCursorAfter,
            null,
            false);
   }
   
   public void executeSelection(boolean consoleExecuteWhenNotFocused,
         boolean moveCursorAfter,
         String functionWrapper,
         boolean onlyUseConsole)
   {
      // when executing LaTeX in R Markdown, show a popup preview
      if (executeLatex(false))
         return;
      
      // when executing inline R code, show a popup preview
      if (executeInlineChunk())
         return;
      
      Range selectionRange = docDisplay_.getSelectionRange();
      boolean noSelection = selectionRange.isEmpty();
      if (noSelection)
      {
         // don't do multiline execution within Roxygen examples
         int row = docDisplay_.getSelectionStart().getRow();
         if (isRoxygenExampleRow(row))
         {
            selectionRange = Range.fromPoints(
                  Position.create(row, 0),
                  Position.create(row, docDisplay_.getLength(row)));
         }
         else
         {
            // if no selection, follow UI pref to see what to execute
            // (Python code is always submitted line-by-line for now as
            // we need special code to understand Python code structure still)
            if (DocumentMode.isCursorInPythonMode(docDisplay_))
            {
               selectionRange = Range.fromPoints(
                     Position.create(row, 0),
                     Position.create(row, docDisplay_.getLength(row)));
            }
            else
            {
               selectionRange = getRangeFromBehavior(
                     prefs_.executionBehavior().getValue());
            }
         }
         
         // if we failed to discover a range, bail
         if (selectionRange == null)
            return;
         
         // make it harder to step off the end of a chunk
         InsertChunkInfo insert = docDisplay_.getInsertChunkInfo();
         if (insert != null && !StringUtil.isNullOrEmpty(insert.getValue()))
         {
            // get the selection we're about to execute; if it's the same as
            // the last line of the chunk template, don't run it
            String code = codeExtractor_.extractCode(docDisplay_, selectionRange);
            String[] chunkLines = insert.getValue().split("\n");
            if (!StringUtil.isNullOrEmpty(code) &&
                chunkLines.length > 0 &&
                code.trim() == chunkLines[chunkLines.length - 1].trim())
               return;
         }
      }

      executeRange(selectionRange, functionWrapper, onlyUseConsole);
      
      // advance if there is no current selection
      if (noSelection && moveCursorAfter)
      {
         if (DocumentMode.isCursorInPythonMode(docDisplay_))
         {
            // don't skip empty / blank lines when executing Python line-by-line
            Position nextPos = Position.create(
                  docDisplay_.getCursorPosition().getRow() + 1,
                  0);
            docDisplay_.setCursorPosition(nextPos);
         }
         else
         {
            moveCursorAfterExecution(selectionRange, true);
         }
      }
   }
   
   public void executeSelection(boolean consoleExecuteWhenNotFocused)
   {
      executeSelectionMaybeNoFocus(consoleExecuteWhenNotFocused,
            true,
            null,
            false);
   }
   
   public void runSelectionAsJob(boolean useJobLauncher)
   {
      Range selectionRange = docDisplay_.getSelectionRange();
      boolean noSelection = selectionRange.isEmpty();
      if (noSelection)
      {
         // nothing to execute; don't guess
         return;
      }
      String code = codeExtractor_.extractCode(docDisplay_, selectionRange);
      String targetPath = target_ == null ? null : target_.getPath();
      if (useJobLauncher)
         events_.fireEvent(new LauncherJobRunSelectionEvent(targetPath, code));
      else
         events_.fireEvent(new JobRunSelectionEvent(targetPath, code));
   }
   
   public void executeRange(Range range)
   {
      executeRange(range, null, false);
   }
   
   public void sendSelectionToTerminal(boolean skipBlankLines)
   {
      // send selection from this editor to the terminal
      Range selectionRange = docDisplay_.getSelectionRange();
      boolean noSelection = selectionRange.isEmpty();
      if (noSelection)
      {
         // current line
         int row = docDisplay_.getSelectionStart().getRow();
         selectionRange = Range.fromPoints(
               Position.create(row, 0),
               Position.create(row, docDisplay_.getLength(row)));
      }
      String code = codeExtractor_.extractCode(docDisplay_, selectionRange);
      if (!(code.endsWith("\n") || code.endsWith("\r")))
         code = code + "\n";
      events_.fireEvent(new SendToTerminalEvent(code, prefs_.focusConsoleAfterExec().getValue()));

      if (noSelection)
      {
         moveCursorAfterExecution(selectionRange, skipBlankLines);
      }
   }

   public void openNewTerminalHere()
   {
      if (target_ == null)
         return;
      if (StringUtil.isNullOrEmpty(target_.getPath()))
         return;
      FileSystemItem file = FileSystemItem.createFile(target_.getPath());
      events_.fireEvent(new CreateNewTerminalEvent(file.getParentPathString()));
   }

   public void sendFilenameToTerminal()
   {
      if (target_ == null)
         return;
      String filename = target_.getPath();
      if (StringUtil.isNullOrEmpty(filename))
         filename = target_.getName().getValue();
      if (!StringUtil.isNullOrEmpty(filename))
         events_.fireEvent(new SendToTerminalEvent(filename, true /*focus terminal*/));
   }

   public void profileSelection()
   {
      // allow console a chance to execute code if we aren't focused
      if (!docDisplay_.isFocused())
      {
         events_.fireEvent(new ConsoleExecutePendingInputEvent(
               commands_.profileCodeWithoutFocus().getId()));
         return;
      }
      
      executeSelection(false, false, "profvis::profvis", true);
   }
   
   private void executeSelectionMaybeNoFocus(boolean consoleExecuteWhenNotFocused,
                                             boolean moveCursorAfter,
                                             String functionWrapper,
                                             boolean onlyUseConsole)
   {
      // allow console a chance to execute code if we aren't focused
      if (consoleExecuteWhenNotFocused && !docDisplay_.isFocused())
      {
         events_.fireEvent(new ConsoleExecutePendingInputEvent(
               commands_.executeCodeWithoutFocus().getId()));
         return;
      }

      executeSelection(consoleExecuteWhenNotFocused, moveCursorAfter, functionWrapper, false);
   }
   
   private void executeRange(Range range, String functionWrapper, boolean onlyUseConsole)
   {
      String code = codeExtractor_.extractCode(docDisplay_, range);
      String language = "R";
     
      setLastExecuted(range.getStart(), range.getEnd());
      
      // trim intelligently (don't trim blank lines for Python
      // since those lines could signal the end of a block)
      if (!DocumentMode.isSelectionInPythonMode(docDisplay_))
      {
         code = StringUtil.trimBlankLines(code);
      }
      else
      {
         language = "Python";
      }
      
      // strip roxygen off the beginning of lines
      if (isRoxygenExampleRange(range))
      {
         code = code.replaceFirst("^[ \\t]*#'[ \\t]?", "");
         code = code.replaceAll("\n[ \\t]*#'[ \\t]?", "\n");
      }
      
      // if we're in a chunk with in-line output, execute it there instead
      if (!onlyUseConsole && docDisplay_.showChunkOutputInline())
      {
         Scope scope = docDisplay_.getCurrentChunk(range.getStart());
         if (scope != null)
         {
            events_.fireEvent(new SendToChunkConsoleEvent(docId_, 
                  scope, range));
            return;
         }
      }
      
      if (functionWrapper != null)
      {
         code = functionWrapper + "({" + code + "})";
      }
      
      // send to console
      events_.fireEvent(new SendToConsoleEvent(
                                  code,
                                  language,
                                  true,
                                  true,
                                  prefs_.focusConsoleAfterExec().getValue(),
                                  false));
   }
   
   public void executeBehavior(String executionBehavior)
   {
      Range range = getRangeFromBehavior(executionBehavior);
      executeRange(range, null, false);
      moveCursorAfterExecution(range, true);
   }
   
   private Range getRangeFromBehavior(String executionBehavior)
   {
      Range range;
      
      // by default the range can encompass the whole document
      int startRowLimit = 0;
      int endRowLimit = docDisplay_.getRowCount();
      
      // limit range to chunk if we're inside one
      Scope scope = docDisplay_.getCurrentChunk();
      if (scope != null)
      {
        
         startRowLimit = scope.getBodyStart().getRow();
         endRowLimit = scope.getEnd().getRow() - 1;
      }
  
      if (executionBehavior == UserPrefs.EXECUTION_BEHAVIOR_STATEMENT)
      {
         // no scope to guard region, check the document itself to find
         // the region to execute
         range = docDisplay_.getMultiLineExpr(
               docDisplay_.getCursorPosition(), startRowLimit, endRowLimit);
         
         // expand to include comments (10 lines max)
         int startRow = range.getStart().getRow();
         for (int i = 0; i < 10; i++)
         {
            if (startRow == 0)
               break;
            
            String line = docDisplay_.getLine(startRow - 1);
            Pattern pattern = Pattern.create("^\\s*#");
            if (!pattern.test(line))
               break;
            
            startRow--;
         }
         range.getStart().setRow(startRow);
      }
      else if (executionBehavior == UserPrefs.EXECUTION_BEHAVIOR_PARAGRAPH)
      {
         range = docDisplay_.getParagraph(
               docDisplay_.getCursorPosition(), startRowLimit, endRowLimit);
      }
      else
      {
         // single-line execution
         int row = docDisplay_.getCursorPosition().getRow();
         range = Range.fromPoints(
               Position.create(row, 0),
               Position.create(row, docDisplay_.getLength(row)));
      }
      return range;
   }
   
   public void executeLastCode()
   {
      if (lastExecutedCode_ != null)
      {
         String code = lastExecutedCode_.getValue();
         if (code != null && code.trim().length() > 0)
         {
            // if in notebook mode, we want to execute the code inside the 
            // chunk rather than at the console
            Scope scope = null;
            if (docDisplay_.showChunkOutputInline())
            {
               scope = docDisplay_.getCurrentChunk(
                  lastExecutedCode_.getRange().getStart());
            }

            if (scope == null)
            {
               events_.fireEvent(new SendToConsoleEvent(code, true));
            }
            else
            {
               events_.fireEvent(new SendToChunkConsoleEvent(docId_, 
                     scope, lastExecutedCode_.getRange()));
            }
         }
      }
   }
   
   public void setLastExecuted(Position start, Position end)
   {
      detachLastExecuted();
      lastExecutedCode_ = docDisplay_.createAnchoredSelection(start, end);
   }

   public void detachLastExecuted()
   {
      if (lastExecutedCode_ != null)
      {
         lastExecutedCode_.detach();
         lastExecutedCode_ = null;
      }
   }
   
   private boolean isRoxygenExampleRow(int row)
   {
      // walk back until we find '@examples' or a non-roxygen line
      for (int i = row; i >= 0; i--)
      {
         String line = docDisplay_.getLine(i);
         
         if (!isRoxygenLine(line))
            return false;
         
         if (line.matches("^\\s*#+'\\s*@example.*$"))
            return true;
      }
      
      return false;
   }
   
   private boolean isRoxygenExampleRange(Range range)
   {
      // ensure all of the lines in the selection are within roxygen
      int selStartRow = range.getStart().getRow();
      int selEndRow = range.getEnd().getRow();
      
      // ignore the last row if it's column 0
      if (range.getEnd().getColumn() == 0)
         selEndRow = Math.max(selEndRow-1, selStartRow);
      
      for (int i = selStartRow; i <= selEndRow; i++)
      {
         if (!isRoxygenLine(docDisplay_.getLine(i)))
            return false;
      }
      
      // scan backwards and look for @example
      int row = selStartRow;
      while (--row >= 0)
      {
         String line = docDisplay_.getLine(row);
         
         // must still be within roxygen
         if (!isRoxygenLine(line))
            return false;
         
         // if we are in an example block return true
         if (line.matches("^\\s*#'\\s*@example.*$"))
            return true;
      }
      
      // didn't find the example block
      return false;
   }
   
   private boolean isRoxygenLine(String line)
   {
      String trimmedLine = line.trim();
      return (trimmedLine.length() == 0) || trimmedLine.startsWith("#'");
   }
   
   private boolean executeLatex(boolean background)
   {
      // need a suitable editing target to render LaTeX chunks
      if (target_ == null)
         return false;
      
      Range range = MathJaxUtil.getLatexRange(docDisplay_);
      if (range == null)
         return false;
      target_.renderLatex(range, background);
      return true;
   }
   
   private boolean executeInlineChunk()
   {
      if (!docDisplay_.getSelection().isEmpty())
         return false;
      
      Token token = docDisplay_.getTokenAt(docDisplay_.getCursorPosition());
      if (token == null || !token.hasType("inline_r_chunk"))
         return false;
      
      // construct range to execute, trimming off the "`r ...`" boundaries
      int row = docDisplay_.getCursorPosition().getRow();
      int startColumn = token.getColumn() + 3;
      int endColumn   = token.getColumn() + token.getValue().length() - 1;
      Range range = Range.create(row, startColumn, row, endColumn);
      
      inlineChunkExecutor_.execute(range);
      return true;
   }
   
   private void moveCursorAfterExecution(Range selectionRange, boolean skipBlankLines)
   {
      docDisplay_.setCursorPosition(Position.create(
            selectionRange.getEnd().getRow(), 0));
      if (!docDisplay_.moveSelectionToNextLine(skipBlankLines))
         docDisplay_.moveSelectionToBlankLine();
      docDisplay_.scrollCursorIntoViewIfNecessary(3);
   }
   
   private final DocDisplay docDisplay_;
   private final TextEditingTarget target_;
   private final CodeExtractor codeExtractor_;
   private final String docId_;
   private final EditingTargetInlineChunkExecution inlineChunkExecutor_;
   private AnchoredSelection lastExecutedCode_;
   
   // Injected ----
   private EventBus events_;
   private UserPrefs prefs_;
   private Commands commands_;
   @SuppressWarnings("unused")
   private SourceServerOperations server_;
}

