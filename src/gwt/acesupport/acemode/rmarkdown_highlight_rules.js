/*
 * markdown_highlight_rules.js
 *
 * Copyright (C) 2009-12 by RStudio, PBC
 *
 * The Initial Developer of the Original Code is
 * Ajax.org B.V.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
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
define("mode/rmarkdown_highlight_rules", ["require", "exports", "module"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var RHighlightRules = require("mode/r_highlight_rules").RHighlightRules;
var c_cppHighlightRules = require("mode/c_cpp_highlight_rules").c_cppHighlightRules;
var PerlHighlightRules = require("ace/mode/perl_highlight_rules").PerlHighlightRules;
var PythonHighlightRules = require("mode/python_highlight_rules").PythonHighlightRules;
var RubyHighlightRules = require("ace/mode/ruby_highlight_rules").RubyHighlightRules;
var MarkdownHighlightRules = require("mode/markdown_highlight_rules").MarkdownHighlightRules;
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;
var YamlHighlightRules = require("mode/yaml_highlight_rules").YamlHighlightRules;
var ShHighlightRules = require("mode/sh_highlight_rules").ShHighlightRules;
var StanHighlightRules = require("mode/stan_highlight_rules").StanHighlightRules;
var SqlHighlightRules = require("mode/sql_highlight_rules").SqlHighlightRules;
var JavaScriptHighlightRules = require("ace/mode/javascript_highlight_rules").JavaScriptHighlightRules;
var Utils = require("mode/utils");

function makeDateRegex()
{
   var months = ["January", "February", "March", "April", "May", "June",
                 "July", "August", "September", "October", "November", "December"];

   var reString =
          "((?:" + months.join("|") + ")\\s+\\d+(?:st|nd|rd|s|n|r)?(?:\\s*(?:,)?(?:\\s*\\d+)?)?)";

   return new RegExp(reString);
}

var RMarkdownHighlightRules = function() {

   // Base rule set (markdown)
   this.$rules = new MarkdownHighlightRules().getRules();

   // use 'firstLine' rule so that YAML rules can apply only there
   this.$rules["firstLine"] = this.$rules["allowBlock"].slice();
   
   // Embed R highlight rules
   Utils.embedRules(
      this,
      RHighlightRules,
      "r",
      this.$reChunkStartString,
      this.$reChunkEndString,
      ["start", "listblock", "allowBlock"]
   );

   // Embed C++ highlight rules
   Utils.embedRules(
      this,
      c_cppHighlightRules,
      "r-cpp",
      this.$reCppChunkStartString,
      this.$reChunkEndString,
      ["start", "listblock", "allowBlock"]
   );

   // Embed perl highlight rules
   Utils.embedRules(
      this,
      PerlHighlightRules,
      "perl",
      this.$rePerlChunkStartString,
      this.$reChunkEndString,
      ["start", "listblock", "allowBlock"]
   );

   // Embed python highlight rules
   Utils.embedRules(
      this,
      PythonHighlightRules,
      "python",
      this.$rePythonChunkStartString,
      this.$reChunkEndString,
      ["start", "listblock", "allowBlock"]
   );

   // Embed ruby highlight rules
   Utils.embedRules(
      this,
      RubyHighlightRules,
      "ruby",
      this.$reRubyChunkStartString,
      this.$reChunkEndString,
      ["start", "listblock", "allowBlock"]
   );

   // Embed Markdown highlight rules (for bookdown)
   Utils.embedRules(
      this,
      MarkdownHighlightRules,
      "markdown",
      this.$reMarkdownChunkStartString,
      this.$reChunkEndString,
      ["start", "listblock", "allowBlock"]
   );

   // Embed shell scripting highlight rules (sh, bash)
   Utils.embedRules(
       this,
       ShHighlightRules,
       "sh",
       this.$reShChunkStartString,
       this.$reChunkEndString,
       ["start", "listblock", "allowBlock"]
   );

   // Embed stan highlighting rules
   Utils.embedRules(
       this,
       StanHighlightRules,
       "stan",
       this.$reStanChunkStartString,
       this.$reChunkEndString,
       ["start", "listblock", "allowBlock"]
   );

   // Embed sql highlighting rules
   Utils.embedRules(
       this,
       SqlHighlightRules,
       "sql",
       this.$reSqlChunkStartString,
       this.$reChunkEndString,
       ["start", "listblock", "allowBlock"]
   );

   // Embed JavaScript highlighting rules
   Utils.embedRules(
       this,
       JavaScriptHighlightRules,
       "js",
       this.$reJavaScriptChunkStartString,
       this.$reChunkEndString,
       ["start", "listblock", "allowBlock"]
   );

   // Embed YAML highlighting rules
   Utils.embedRules(
      this,
      YamlHighlightRules,
      "yaml",
      "^\\s*---\\s*$",
      "^\\s*(?:---|\\.\\.\\.)\\s*$",
      ["firstLine"]
   );

   this.$rules["yaml-start"].unshift({
      token: ["string"],
      regex: makeDateRegex()
   });

   this.$rules["yaml-start"].push({
      defaultToken: "text.nospell"
   });

   this.normalizeRules();
};
oop.inherits(RMarkdownHighlightRules, TextHighlightRules);

(function() {

   function engineRegex(engine) {
      return "^(?:[ ]{4})?`{3,}\\s*\\{[Rr]\\b(?:.*)engine\\s*\\=\\s*['\"]" + engine + "['\"](?:.*)\\}\\s*$|" +
         "^(?:[ ]{4})?`{3,}\\s*\\{" + engine + "\\b(?:.*)\\}\\s*$";
   }

   this.$reChunkStartString =
      "^(?:[ ]{4})?`{3,}\\s*\\{\\w+\\b(.*)\\}\\s*$";

   this.$reChunkEndString =
      "^(?:[ ]{4})?`{3,}\\s*$";

   this.$reCppChunkStartString        = engineRegex("[Rr]cpp");
   this.$reMarkdownChunkStartString   = engineRegex("block");
   this.$rePerlChunkStartString       = engineRegex("perl");
   this.$rePythonChunkStartString     = engineRegex("python");
   this.$reRubyChunkStartString       = engineRegex("ruby");
   this.$reShChunkStartString         = engineRegex("(?:bash|sh)");
   this.$reStanChunkStartString       = engineRegex("stan");
   this.$reSqlChunkStartString        = engineRegex("sql");
   this.$reJavaScriptChunkStartString = engineRegex("(?:d3|js)");
   
}).call(RMarkdownHighlightRules.prototype);

exports.RMarkdownHighlightRules = RMarkdownHighlightRules;
});
