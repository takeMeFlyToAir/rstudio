/*
 * MenuEmitter.java
 *
 * Copyright (C) 2009-12 by RStudio, PBC
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
package org.rstudio.core.rebind.command;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.PrintWriter;

public class MenuEmitter
{
   public MenuEmitter(TreeLogger logger,
                      GeneratorContext context,
                      JClassType bundleType,
                      Element menuEl) throws UnableToCompleteException
   {
      logger_ = logger;
      context_ = context;
      bundleType_ = bundleType;
      menuEl_ = menuEl;
      menuId_ = menuEl.getAttribute("id");
      if (menuId_.length() == 0)
      {
         logger.log(TreeLogger.Type.ERROR, "Menu must have an id attribute");
         throw new UnableToCompleteException();
      }
      packageName_ = bundleType.getPackage().getName();
   }

   public String generate() throws UnableToCompleteException
   {
      String className = bundleType_.getSimpleSourceName() + "__Menu_" + menuId_;

      PrintWriter printWriter = context_.tryCreate(logger_,
                                                  packageName_,
                                                  className);
      if (printWriter == null)
         return null;

      ClassSourceFileComposerFactory factory =
            new ClassSourceFileComposerFactory(packageName_, className);
      factory.addImport("org.rstudio.core.client.Debug");
      factory.addImport("org.rstudio.core.client.command.MenuCallback");
      SourceWriter writer = factory.createSourceWriter(context_, printWriter);

      emitFields(writer);
      emitConstructor(writer, className);
      emitMethod(writer);
      writer.outdent();
      writer.println("}");
      context_.commit(logger_, printWriter);

      return packageName_ + "." + className;
   }

   private void emitFields(SourceWriter writer)
   {
      writer.println("private "
                     + bundleType_.getQualifiedSourceName()
                     + " cmds;");
   }

   private void emitConstructor(SourceWriter writer, String className)
   {
      writer.println("public " + className +  "("
                     + bundleType_.getQualifiedSourceName() + " commands) {");
      writer.indentln("this.cmds = commands;");
      writer.println("}");
   }

   private void emitMethod(SourceWriter writer) throws UnableToCompleteException
   {
      writer.println("public void createMenu(MenuCallback callback) {");
      writer.indent();

      writer.println("callback.beginMainMenu();");
      // Vertical defaults to true
      emitMenu(writer, menuEl_);
      writer.println("callback.endMainMenu();");

      writer.outdent();
      writer.println("}");
   }

   private void emitMenu(SourceWriter writer, Element el) throws UnableToCompleteException
   {
      for (Node n = el.getFirstChild(); n != null; n = n.getNextSibling())
      {
         if (n.getNodeType() != Node.ELEMENT_NODE)
            continue;

         Element child = (Element)n;

         if (child.getTagName().equals("cmd"))
         {
            String cmdId = child.getAttribute("refid");
            writer.print("callback.addCommand(");
            writer.print("\"" + Generator.escape(cmdId) + "\", ");
            writer.println("this.cmds." + cmdId + "());");
         }
         else if (child.getTagName().equals("separator"))
         {
            writer.println("callback.addSeparator();");
         }
         else if (child.getTagName().equals("menu"))
         {
            String label = child.getAttribute("label");
            writer.println("callback.beginMenu(\"" +
                           Generator.escape(label) +
                           "\");");
            emitMenu(writer, child);
            writer.println("callback.endMenu();");
         }
         else if (child.getTagName().equals("dynamic"))
         {
            String dynamicClass = child.getAttribute("class");
            writer.println("new " + dynamicClass + "().execute(callback);");
         }
         else
         {
            logger_.log(TreeLogger.Type.ERROR,
                        "Unexpected tag " + el.getTagName() + " in menu");
            throw new UnableToCompleteException();
         }
      }
   }

   private final TreeLogger logger_;
   private final GeneratorContext context_;
   private final JClassType bundleType_;
   private final String menuId_;
   private final Element menuEl_;
   private final String packageName_;
}
