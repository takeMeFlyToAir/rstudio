/*
 * ConnectionHistory.cpp
 *
 * Copyright (C) 2009-16 by RStudio, PBC
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

#include "ConnectionHistory.hpp"

#include <core/FileSerializer.hpp>


#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace connections {


namespace {


bool isConnection(const ConnectionId& id, json::Value valueJson)
{
   if (!json::isType<json::Object>(valueJson))
   {
      LOG_WARNING_MESSAGE("Connection JSON has unexpected format");
      return false;
   }

   const json::Object& connJson = valueJson.getObject();
   return hasConnectionId(id, connJson);
}


} // anonymous namespace



ConnectionHistory& connectionHistory()
{
   static ConnectionHistory instance;
   return instance;
}

ConnectionHistory::ConnectionHistory()
{
}

Error ConnectionHistory::initialize()
{
   // register to be notified when connections are changed
   connectionsDir_ = module_context::registerMonitoredUserScratchDir(
            "connection_history",
            boost::bind(&ConnectionHistory::onConnectionsChanged, this));

   return Success();
}

void ConnectionHistory::update(const Connection& connection)
{
   // read existing connections
   json::Array connectionsJson;
   Error error = readConnections(&connectionsJson);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // look for a matching connection and update it
   bool foundConnection = false;
   for (size_t i = 0; i<connectionsJson.getSize(); i++)
   {
      json::Value valueJson = connectionsJson[i];
      if (isConnection(connection.id, valueJson))
      {
         connectionsJson[i] = connectionJson(connection);
         foundConnection = true;
         break;
      }
   }

   // if we didn't find a connection then append
   if (!foundConnection)
      connectionsJson.push_back(connectionJson(connection));

   // write out the connections
   error = writeConnections(connectionsJson);
   if (error)
      LOG_ERROR(error);

   // fire event
   onConnectionsChanged();
}


void ConnectionHistory::remove(const ConnectionId &id)
{
   // read existing connections
   json::Array connectionsJson;
   Error error = readConnections(&connectionsJson);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // remove matching connection
   connectionsJson.erase(std::remove_if(connectionsJson.begin(),
                                        connectionsJson.end(),
                                        boost::bind(isConnection, id, _1)),
                         connectionsJson.end());

   // write out the connections
   error = writeConnections(connectionsJson);
   if (error)
      LOG_ERROR(error);
}



json::Array ConnectionHistory::connectionsAsJson()
{
   json::Array connectionsJson;
   Error error = readConnections(&connectionsJson);
   if (error)
      LOG_ERROR(error);
   return connectionsJson;
}

void ConnectionHistory::onConnectionsChanged()
{
   ClientEvent event(client_events::kConnectionListChanged,
                     connectionsAsJson());
   module_context::enqueClientEvent(event);
}

const char* const kConnectionListFile = "connection-history-database.json";

Error ConnectionHistory::readConnections(json::Array* pConnections)
{
   FilePath connectionListFile = connectionsDir_.completeChildPath(kConnectionListFile);
   if (connectionListFile.exists())
   {
      std::string contents;
      Error error = core::readStringFromFile(connectionListFile, &contents);
      if (error)
         return error;

      json::Value parsedJson;
      if (parsedJson.parse(contents) ||
          !json::isType<json::Array>(parsedJson))
      {
         return systemError(boost::system::errc::protocol_error,
                            "Error parsing connections json file",
                            ERROR_LOCATION);
      }

      *pConnections = parsedJson.getValue<json::Array>();
   }

   return Success();
}

Error ConnectionHistory::writeConnections(const json::Array& connectionsJson)
{
   FilePath connectionListFile = connectionsDir_.completeChildPath(kConnectionListFile);
   std::shared_ptr<std::ostream> pStream;
   Error error = connectionListFile.openForWrite(pStream);
   if (error)
      return error;

   connectionsJson.writeFormatted(*pStream);

   return Success();
}



} // namespace connections
} // namespace modules
} // namespace session
} // namespace rstudio

