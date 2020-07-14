/*
 * DesktopSubMenu.hpp
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

#ifndef DESKTOP_SUBMENU_HPP
#define DESKTOP_SUBMENU_HPP

#include <QObject>
#include <QHash>
#include <QList>
#include <QMenu>
#include <QKeyEvent>

namespace rstudio {
namespace desktop {

class SubMenu : public QMenu
{
   Q_OBJECT
public:
   explicit SubMenu(const QString& title, QWidget* parent = nullptr);

protected Q_SLOTS:
   void onAboutToShow();
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_SUBMENU_HPP
