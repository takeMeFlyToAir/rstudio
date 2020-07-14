/*
 * FileMode.hpp
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


#ifndef CORE_SYSTEM_FILE_MODE_HPP
#define CORE_SYSTEM_FILE_MODE_HPP

#ifdef _WIN32
#error FileMode.hpp is is not supported on Windows
#endif

#include <sys/stat.h>
#include <sys/unistd.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>

namespace rstudio {
namespace core {
namespace system {

enum FileMode
{
   UserReadWriteMode,
   UserReadWriteExecuteMode,
   UserReadWriteGroupReadMode,
   UserReadWriteGroupEveryoneReadMode,
   UserReadWriteExecuteGroupEveryoneReadExecuteMode,
   EveryoneReadMode,
   EveryoneReadWriteMode,
   EveryoneReadWriteExecuteMode
};

namespace {

int octalStrToFileMode(const std::string& fileModeStr)
{
   return safe_convert::stringTo<int>(fileModeStr, 0666, std::oct);
}

} // anonymous namespace

inline Error changeFileMode(const FilePath& filePath, mode_t mode)
{
   // change the mode
   errno = 0;
   if (::chmod(filePath.getAbsolutePath().c_str(), mode) < 0)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", filePath);
      return error;
   }
   else
      return Success();
}

inline Error changeFileMode(const FilePath& filePath,
                            const std::string& fileModeStr)
{
   return changeFileMode(filePath, octalStrToFileMode(fileModeStr));
}

inline Error changeFileMode(const FilePath& filePath,
                            FileMode fileMode,
                            bool stickyBit)
{
   mode_t mode ;
   switch(fileMode)
   {
      case UserReadWriteMode:
         mode = S_IRUSR | S_IWUSR;
         break;

      case UserReadWriteExecuteMode:
         mode = S_IRUSR | S_IWUSR | S_IXUSR;
         break;

      case UserReadWriteGroupReadMode:
         mode = S_IRUSR | S_IWUSR | S_IRGRP ;
         break;

      case UserReadWriteGroupEveryoneReadMode:
         mode =  S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH;
         break;

      case UserReadWriteExecuteGroupEveryoneReadExecuteMode:
         mode = S_IRWXU | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH;
         break;

      case EveryoneReadMode:
         mode = S_IRUSR | S_IRGRP | S_IROTH;
         break;

      case EveryoneReadWriteMode:
         mode = S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH;
         break;

      case EveryoneReadWriteExecuteMode:
         mode = S_IRWXU | S_IRWXG | S_IRWXO;
         break;

      default:
         return systemError(ENOTSUP, ERROR_LOCATION);
   }

   // check for sticky bit
   if (stickyBit)
      mode |= S_ISVTX;

   return changeFileMode(filePath, mode);
}

inline Error changeFileMode(const FilePath& filePath, FileMode fileMode)
{
   return changeFileMode(filePath, fileMode, false);
}

inline Error getFileMode(const FilePath& filePath, FileMode* pFileMode)
{
   struct stat st;
   if (::stat(filePath.getAbsolutePath().c_str(), &st) == -1)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", filePath);
      return error;
   }

   // extract the bits
   std::string mode(9, '-');
   if ( st.st_mode & S_IRUSR ) mode[0] = 'r';
   if ( st.st_mode & S_IWUSR ) mode[1] = 'w';
   if ( st.st_mode & S_IXUSR ) mode[2] = 'x';

   if ( st.st_mode & S_IRGRP ) mode[3] = 'r';
   if ( st.st_mode & S_IWGRP ) mode[4] = 'w';
   if ( st.st_mode & S_IXGRP ) mode[5] = 'x';

   if ( st.st_mode & S_IROTH ) mode[6] = 'r';
   if ( st.st_mode & S_IWOTH ) mode[7] = 'w';
   if ( st.st_mode & S_IXOTH ) mode[8] = 'x';

   if (mode ==      "rw-------")
      *pFileMode = UserReadWriteMode;
   else if (mode == "rwx------")
      *pFileMode = UserReadWriteExecuteMode;
   else if (mode == "rw-r-----")
      *pFileMode = UserReadWriteGroupReadMode;
   else if (mode == "rw-r--r--")
      *pFileMode = UserReadWriteGroupEveryoneReadMode;
   else if (mode == "r--r--r--")
      *pFileMode = EveryoneReadMode;
   else if (mode == "rw-rw-rw-")
      *pFileMode = EveryoneReadWriteMode;
   else if (mode == "rwxrwxrwx")
      *pFileMode = EveryoneReadWriteExecuteMode;
   else if (mode == "rwxr-xr-x")
      *pFileMode = UserReadWriteExecuteGroupEveryoneReadExecuteMode;
   else
       return systemError(boost::system::errc::not_supported, ERROR_LOCATION);

   return Success();
}

inline Error isFileReadable(const FilePath& filePath, bool* pReadable)
{
   int result = ::access(filePath.getAbsolutePath().c_str(), R_OK);
   if (result == 0) 
   {
      // user has access
      *pReadable = true;
   }
   else if (errno == EACCES)
   {
      // this error is expected when the user doesn't have access to the path
      *pReadable = false;
   }
   else 
   {
      // some other error (unexpected)
      return systemError(errno, ERROR_LOCATION);
   }
   return Success();
}

inline Error isFileWriteable(const FilePath& filePath, bool* pWriteable)
{
   int result = ::access(filePath.getAbsolutePath().c_str(), W_OK);
   if (result == 0)
   {
      // user has access
      *pWriteable = true;
   }
   else if (errno == EACCES)
   {
      *pWriteable = false;
   }
   else
   {
      return systemError(errno, ERROR_LOCATION);
   }
   return Success();
}


} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_FILE_MODE_HPP
