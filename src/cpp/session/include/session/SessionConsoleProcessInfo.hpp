/*
 * SessionConsoleProcessInfo.hpp
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
#ifndef SESSION_CONSOLE_PROCESS_INFO_HPP
#define SESSION_CONSOLE_PROCESS_INFO_HPP

#include <boost/circular_buffer.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <shared_core/FilePath.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Process.hpp>
#include <core/system/Types.hpp>

#include <session/SessionTerminalShell.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace console_process {

enum InteractionMode
{
   InteractionNever = 0,
   InteractionPossible = 1,
   InteractionAlways = 2
};

enum ChannelMode
{
   Rpc = 0,
   Websocket = 1,
};

enum AutoCloseMode
{
   DefaultAutoClose = 0, // obey user preference
   AlwaysAutoClose = 1, // always auto-close
   NeverAutoClose = 2, // never auto-close
   CleanExitAutoClose = 3, // auto-close only if shell returned zero exit code
};

enum SerializationMode
{
   ClientSerialization = 0, // serialize for front-end client
   PersistentSerialization = 1  // serialize for persistent storage
};

extern const int kDefaultMaxOutputLines;
extern const int kDefaultTerminalMaxOutputLines;
extern const int kNoTerminal;
extern const int kNewTerminal;
extern const size_t kOutputBufferSize;

// ConsoleProcess metadata that is persisted, and sent to the client on
// create/reconnect
class ConsoleProcessInfo : boost::noncopyable,
                           public boost::enable_shared_from_this<ConsoleProcessInfo>
{
private:
   // This constructor is only for resurrecting orphaned processes (i.e. for
   // suspend/resume scenarios)
   ConsoleProcessInfo();

public:
   // constructor for interactive terminals
   ConsoleProcessInfo(
         const std::string& caption,
         const std::string& title,
         const std::string& handle,
         int terminalSequence,
         TerminalShell::ShellType shellType,
         bool altBufferActive,
         const core::FilePath& cwd,
         int cols, int rows, bool zombie, bool trackEnv);

   // constructor for non-terminals
   ConsoleProcessInfo(
         const std::string& caption,
         InteractionMode mode,
         int maxOutputLines = kDefaultMaxOutputLines);

   virtual ~ConsoleProcessInfo() = default;

   // Caption is shown on terminal tabs, e.g. Terminal 1
   void setCaption(std::string& caption) { caption_ = caption; }
   std::string getCaption() const { return caption_; }

   // Title is set by terminal escape sequence, typically to show current dir
   void setTitle(std::string& title) { title_ = title; }
   std::string getTitle() const { return title_; }

   // Handle client uses to refer to this process
   void ensureHandle();
   std::string getHandle() const { return handle_; }

   // Sequence number of the associated terminal; used to control display
   // order of terminal tabs; constant 'kNoTerminal' indicates a non-terminal
   void setTerminalSequence(int sequence) { terminalSequence_ = sequence; }
   int getTerminalSequence() const { return terminalSequence_; }

   // Whether a ConsoleProcess object should start a new process on resume after
   // its process has been killed by a suspend.
   void setAllowRestart(bool allowRestart) { allowRestart_ = allowRestart; }
   bool getAllowRestart() const { return allowRestart_; }

   void setInteractionMode(InteractionMode mode) { interactionMode_ = mode; }
   InteractionMode getInteractionMode() const { return interactionMode_; }

   void setMaxOutputLines(int maxOutputLines) { maxOutputLines_ = maxOutputLines; }
   int getMaxOutputLines() const { return maxOutputLines_; }

   void setShowOnOutput(bool showOnOutput) { showOnOutput_ = showOnOutput; }
   bool getShowOnOutput() const { return showOnOutput_; }

   // Buffer output in case client disconnects/reconnects and needs
   // to recover some history.
   void appendToOutputBuffer(const std::string &str);
   void appendToOutputBuffer(char ch);
   std::string bufferedOutput() const;
   std::string getSavedBufferChunk(int chunk, bool* pMoreAvailable) const;
   std::string getFullSavedBuffer() const;
   int getBufferLineCount() const;
   void deleteLogFile(bool lastLineOnly = false) const;
   void deleteEnvFile() const;
   void saveConsoleEnvironment(const core::system::Options& environment);

   // Has the process exited, and what was the exit code?
   void setExitCode(int exitCode);
   boost::optional<int> getExitCode() const { return exitCode_; }
   void resetExitCode();

   // Does this process have child processes?
   void setHasChildProcs(bool hasChildProcs) { childProcs_ = hasChildProcs; }
   bool getHasChildProcs() const { return childProcs_; }

   // What type of shell is this child process running in?
   TerminalShell::ShellType getShellType() const { return shellType_; }
   void setShellType(TerminalShell::ShellType type) { shellType_ = type; }

   // Type of channel for communicating input/output with client
   ChannelMode getChannelMode() const { return channelMode_; }

   // Mode-dependent identifier for channel
   std::string getChannelId() const { return channelId_; }

   void setChannelMode(ChannelMode mode, const std::string& channelId)
   {
      channelMode_ = mode;
      channelId_ = channelId;
   }

   // Is terminal showing alt-buffer (a full-screen ncurses program)?
   void setAltBufferActive(bool altBufferActive) { altBufferActive_ = altBufferActive; }
   bool getAltBufferActive() const { return altBufferActive_; }

   // Last-known current working directory
   void setCwd(const core::FilePath& cwd) { cwd_ = cwd; }
   core::FilePath getCwd() const { return cwd_; }

   // Last-known terminal dimensions
   void setCols(int cols) { cols_ = cols; }
   void setRows(int rows) { rows_ = rows; }
   int getCols() const { return cols_; }
   int getRows() const { return rows_; }

   // Was terminal session restarted?
   void setRestarted(bool restarted) { restarted_ = restarted; }
   bool getRestarted() const { return restarted_; }

   // Close terminal session after process exits?
   void setAutoClose(AutoCloseMode autoClose) { autoClose_ = autoClose; }
   AutoCloseMode getAutoClose() const { return autoClose_; }

   // Is terminal session a zombie (process exited but keeping buffer)
   void setZombie(bool zombie) { zombie_ = zombie; }
   bool getZombie() const { return zombie_; }

   // Track terminal session's environment?
   void setTrackEnv(bool trackEnv) { trackEnv_ = trackEnv; }
   bool getTrackEnv() const { return trackEnv_; }

   core::json::Object toJson(SerializationMode serialMode) const;
   static boost::shared_ptr<ConsoleProcessInfo> fromJson(const core::json::Object& obj);

   static std::string loadConsoleProcessMetadata();
   static void deleteOrphanedLogs(bool (*validHandle)(const std::string&));
   static void saveConsoleProcesses(const std::string& metadata);
   static void loadConsoleEnvironment(const std::string& handle, core::system::Options* pEnv);

   static AutoCloseMode closeModeFromPref(std::string prefValue);

private:
   std::string caption_;
   std::string title_;
   std::string handle_;
   int terminalSequence_ = kNoTerminal;
   bool allowRestart_ = false;
   InteractionMode interactionMode_ = InteractionNever;
   int maxOutputLines_ = kDefaultMaxOutputLines;
   bool showOnOutput_ = false;
   boost::circular_buffer<char> outputBuffer_ {kOutputBufferSize};
   boost::optional<int> exitCode_;
   bool childProcs_ = true;
   bool altBufferActive_ = false;
   TerminalShell::ShellType shellType_ = TerminalShell::ShellType::Default;
   ChannelMode channelMode_ = Rpc;
   std::string channelId_;
   core::FilePath cwd_;
   int cols_ = core::system::kDefaultCols;
   int rows_ = core::system::kDefaultRows;
   bool restarted_ = false;
   AutoCloseMode autoClose_ = DefaultAutoClose;
   bool zombie_ = false;
   bool trackEnv_ = false;
};

} // namespace console_process
} // namespace session
} // namespace rstudio

#endif // SESSION_CONSOLE_PROCESS_INFO_HPP
