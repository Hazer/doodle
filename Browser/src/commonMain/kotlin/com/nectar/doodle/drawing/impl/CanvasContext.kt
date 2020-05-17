package com.nectar.doodle.drawing.impl

import com.nectar.doodle.Node
import com.nectar.doodle.drawing.Shadow
import com.nectar.doodle.geometry.Size

/**
 * Created by Nicholas Eddy on 10/24/17.
 */

internal interface CanvasContext {
    var size          : Size
    val shadows       : List<Shadow>
    val renderRegion  : Node
    var renderPosition: Node?
}