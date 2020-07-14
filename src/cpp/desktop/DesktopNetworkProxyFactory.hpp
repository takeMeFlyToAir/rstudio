/*
 * DesktopNetworkProxyFactory.hpp
 *
 * Copyright (C) 2009-17 by RStudio, PBC
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

#ifndef DESKTOPNETWORKPROXYFACTORY_HPP
#define DESKTOPNETWORKPROXYFACTORY_HPP

#include <QtCore>
#include <QtNetwork>

class NetworkProxyFactory : public QNetworkProxyFactory
{
public:
    NetworkProxyFactory();

   QList<QNetworkProxy> queryProxy(const QNetworkProxyQuery& query = QNetworkProxyQuery()) override;
};

#endif // DESKTOPNETWORKPROXYFACTORY_HPP
