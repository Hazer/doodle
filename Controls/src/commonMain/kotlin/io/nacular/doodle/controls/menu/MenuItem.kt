package io.nacular.doodle.controls.menu

import io.nacular.doodle.utils.PropertyObservers

/**
 * Created by Nicholas Eddy on 4/30/18.
 */
interface MenuItem {
    // TODO: Naming is sub-optimal here b/c of collisions w/ View
    val parentMenu     : MenuItem?
    val subMenus       : Iterator<MenuItem>
    var menuSelected   : Boolean
    val selectedChanged: PropertyObservers<MenuItem, Boolean>
}
