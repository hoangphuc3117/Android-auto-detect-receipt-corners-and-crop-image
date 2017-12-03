package ppapps.cropreceiptdemo.cropreceipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import org.opencv.core.Point
import ppapps.cropreceiptdemo.R
import java.util.ArrayList
import java.util.HashMap

/**
 * Created by phuchoang on 11/6/17
 */
class PolygonView : FrameLayout {
    private var paint: Paint? = null
    private var pointer1: ImageView? = null
    private var pointer2: ImageView? = null
    private var pointer3: ImageView? = null
    private var pointer4: ImageView? = null
    private var midPointer13: ImageView? = null
    private var midPointer12: ImageView? = null
    private var midPointer34: ImageView? = null
    private var midPointer24: ImageView? = null
    private var polygonView: PolygonView? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        polygonView = this
        pointer1 = getImageView(0, 0)
        pointer2 = getImageView(width, 0)
        pointer3 = getImageView(0, height)
        pointer4 = getImageView(width, height)
        midPointer13 = getImageView(0, height / 2)
        midPointer13!!.setOnTouchListener(MidPointTouchListenerImpl(pointer1!!, pointer3!!))

        midPointer12 = getImageView(0, width / 2)
        midPointer12!!.setOnTouchListener(MidPointTouchListenerImpl(pointer1!!, pointer2!!))

        midPointer34 = getImageView(0, height / 2)
        midPointer34!!.setOnTouchListener(MidPointTouchListenerImpl(pointer3!!, pointer4!!))

        midPointer24 = getImageView(0, height / 2)
        midPointer24!!.setOnTouchListener(MidPointTouchListenerImpl(pointer2!!, pointer4!!))

        addView(pointer1)
        addView(pointer2)
        addView(midPointer13)
        addView(midPointer12)
        addView(midPointer34)
        addView(midPointer24)
        addView(pointer3)
        addView(pointer4)
        initPaint()
    }

    fun getListPoint(): ArrayList<Point> {
        val listPoints = ArrayList<Point>()
        listPoints.add(Point(pointer1!!.x.toDouble() + pointer1!!.width / 2, pointer1!!.y.toDouble() + pointer1!!.height / 2))
        listPoints.add(Point(pointer2!!.x.toDouble() + pointer1!!.width / 2, pointer2!!.y.toDouble() + pointer2!!.height / 2))
        listPoints.add(Point(pointer4!!.x.toDouble() + pointer1!!.width / 2, pointer4!!.y.toDouble() + pointer3!!.height / 2))
        listPoints.add(Point(pointer3!!.x.toDouble() + pointer1!!.width / 2, pointer3!!.y.toDouble() + pointer4!!.height / 2))
        return listPoints
    }

    override fun attachViewToParent(child: View, index: Int, params: ViewGroup.LayoutParams) {
        super.attachViewToParent(child, index, params)
    }

    private fun initPaint() {
        paint = Paint()
        paint!!.color = resources.getColor(R.color.material_deep_teal_200)
        paint!!.strokeWidth = 2f
        paint!!.isAntiAlias = true
    }

    var points: Map<Int, Point>?
        get() {

            val points = ArrayList<Point>()
            points.add(Point(pointer1!!.x.toDouble(), pointer1!!.y.toDouble()))
            points.add(Point(pointer2!!.x.toDouble(), pointer2!!.y.toDouble()))
            points.add(Point(pointer3!!.x.toDouble(), pointer3!!.y.toDouble()))
            points.add(Point(pointer4!!.x.toDouble(), pointer4!!.y.toDouble()))
            return getOrderedPoints(points)
        }
        set(pointFMap) {
            if (pointFMap!!.size == 4) {
                setPointsCoordinates(pointFMap)
            }
        }

    fun getOrderedPoints(points: List<Point>?): Map<Int, Point>? {

        val centerPoint = Point()
        val size = points!!.size
        for (pointF in points) {
            centerPoint.x += pointF.x / size
            centerPoint.y += pointF.y / size
        }
        val orderedPoints = HashMap<Int, Point>()
        for (pointF in points) {
            var index = -1
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3
            }
            orderedPoints.put(index, pointF)
        }
        return orderedPoints
    }

    private fun setPointsCoordinates(pointFMap: Map<Int, Point>) {
        if (pointFMap[2]!!.x.toFloat() - pointer3!!.width / 2 < 0) {
            pointer3!!.x = pointFMap[2]!!.x.toFloat()
        } else {
            pointer3!!.x = pointFMap[2]!!.x.toFloat() - pointer3!!.width / 2
        }
        if (pointFMap[2]!!.y.toFloat() + pointer3!!.height / 2 > polygonView!!.height) {
            pointer3!!.y = pointFMap[2]!!.y.toFloat()
        } else {
            pointer3!!.y = pointFMap[2]!!.y.toFloat() - pointer3!!.height
        }

        if (pointFMap[3]!!.x.toFloat() - pointer4!!.width / 2 < 0) {
            pointer4!!.x = pointFMap[3]!!.x.toFloat()
        } else {
            pointer4!!.x = pointFMap[3]!!.x.toFloat() - pointer4!!.width / 2
        }
        if (pointFMap[3]!!.y.toFloat() + pointer4!!.height / 2 > (polygonView!!.height)) {
            pointer4!!.y = pointFMap[3]!!.y.toFloat()
        } else {
            pointer4!!.y = pointFMap[3]!!.y.toFloat() - pointer4!!.height / 2
        }

        if (pointFMap[0]!!.x.toFloat() - pointer1!!.width / 2 < 0) {
            pointer1!!.x = pointFMap[0]!!.x.toFloat()
        } else {
            pointer1!!.x = pointFMap[0]!!.x.toFloat() - pointer1!!.width / 2
        }
        if (pointFMap[0]!!.y.toFloat() + pointer1!!.height / 2 > (polygonView!!.height)) {
            pointer1!!.y = pointFMap[0]!!.y.toFloat()
        } else {
            pointer1!!.y = pointFMap[0]!!.y.toFloat() - pointer1!!.height / 2
        }

        if (pointFMap[1]!!.x.toFloat() - pointer2!!.width / 2 < 0) {
            pointer2!!.x = pointFMap[1]!!.x.toFloat()
        } else {
            pointer2!!.x = pointFMap[1]!!.x.toFloat() - pointer2!!.width / 2
        }
        if (pointFMap[1]!!.y.toFloat() + pointer2!!.height > (polygonView!!.height)) {
            pointer2!!.y = pointFMap[1]!!.y.toFloat()
        } else {
            pointer2!!.y = pointFMap[1]!!.y.toFloat() - pointer2!!.height / 2
        }

        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        canvas.drawLine(pointer1!!.x + pointer1!!.width / 2, pointer1!!.y + pointer1!!.height / 2, pointer3!!.x + pointer3!!.width / 2, pointer3!!.y + pointer3!!.height / 2, paint!!)
        canvas.drawLine(pointer1!!.x + pointer1!!.width / 2, pointer1!!.y + pointer1!!.height / 2, pointer2!!.x + pointer2!!.width / 2, pointer2!!.y + pointer2!!.height / 2, paint!!)
        canvas.drawLine(pointer2!!.x + pointer2!!.width / 2, pointer2!!.y + pointer2!!.height / 2, pointer4!!.x + pointer4!!.width / 2, pointer4!!.y + pointer4!!.height / 2, paint!!)
        canvas.drawLine(pointer3!!.x + pointer3!!.width / 2, pointer3!!.y + pointer3!!.height / 2, pointer4!!.x + pointer4!!.width / 2, pointer4!!.y + pointer4!!.height / 2, paint!!)
        midPointer13!!.x = pointer3!!.x - (pointer3!!.x - pointer1!!.x) / 2
        midPointer13!!.y = pointer3!!.y - (pointer3!!.y - pointer1!!.y) / 2
        midPointer24!!.x = pointer4!!.x - (pointer4!!.x - pointer2!!.x) / 2
        midPointer24!!.y = pointer4!!.y - (pointer4!!.y - pointer2!!.y) / 2
        midPointer34!!.x = pointer4!!.x - (pointer4!!.x - pointer3!!.x) / 2
        midPointer34!!.y = pointer4!!.y - (pointer4!!.y - pointer3!!.y) / 2
        midPointer12!!.x = pointer2!!.x - (pointer2!!.x - pointer1!!.x) / 2
        midPointer12!!.y = pointer2!!.y - (pointer2!!.y - pointer1!!.y) / 2
    }

    private fun getImageView(x: Int, y: Int): ImageView {
        val imageView = ImageView(context)
        val layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        imageView.layoutParams = layoutParams
        imageView.setImageResource(R.drawable.circle)
        imageView.x = x.toFloat()
        imageView.y = y.toFloat()
        imageView.setOnTouchListener(TouchListenerImpl())
        return imageView
    }

    private inner class MidPointTouchListenerImpl(private val mainPointer1: ImageView, private val mainPointer2: ImageView) : View.OnTouchListener {

        internal var DownPT = PointF() // Record Mouse Position When Pressed Down
        internal var StartPT = PointF() // Record Start Position of 'img'

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val eid = event.action
            when (eid) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = PointF(event.x - DownPT.x, event.y - DownPT.y)

                    if (Math.abs(mainPointer1.x - mainPointer2.x) > Math.abs(mainPointer1.y - mainPointer2.y)) {
                        if (mainPointer2.y + mv.y + v.height.toFloat() < polygonView!!.height && mainPointer2.y + mv.y > 0) {
                            v.x = (StartPT.y + mv.y).toInt().toFloat()
                            StartPT = PointF(v.x, v.y)
                            mainPointer2.y = (mainPointer2.y + mv.y).toInt().toFloat()
                        }
                        if (mainPointer1.y + mv.y + v.height.toFloat() < polygonView!!.height && mainPointer1.y + mv.y > 0) {
                            v.x = (StartPT.y + mv.y).toInt().toFloat()
                            StartPT = PointF(v.x, v.y)
                            mainPointer1.y = (mainPointer1.y + mv.y).toInt().toFloat()
                        }
                    } else {
                        if (mainPointer2.x + mv.x + v.width.toFloat() < polygonView!!.width && mainPointer2.x + mv.x > 0) {
                            v.x = (StartPT.x + mv.x).toInt().toFloat()
                            StartPT = PointF(v.x, v.y)
                            mainPointer2.x = (mainPointer2.x + mv.x).toInt().toFloat()
                        }
                        if (mainPointer1.x + mv.x + v.width.toFloat() < polygonView!!.width && mainPointer1.x + mv.x > 0) {
                            v.x = (StartPT.x + mv.x).toInt().toFloat()
                            StartPT = PointF(v.x, v.y)
                            mainPointer1.x = (mainPointer1.x + mv.x).toInt().toFloat()
                        }
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    DownPT.x = event.x
                    DownPT.y = event.y
                    StartPT = PointF(v.x, v.y)
                }
                MotionEvent.ACTION_UP -> {
                    var color = 0
                    if (isValidShape(points!!)) {
                        color = resources.getColor(R.color.material_deep_teal_200)
                    } else {
                        color = resources.getColor(R.color.colorAccent)
                    }
                    paint!!.color = color
                }
                else -> {
                }
            }
            polygonView!!.invalidate()
            return true
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return super.onTouchEvent(event)
    }

    fun isValidShape(pointFMap: Map<Int, Point>): Boolean {
        return pointFMap.size == 4
    }

    private inner class TouchListenerImpl : View.OnTouchListener {

        internal var DownPT = Point() // Record Mouse Position When Pressed Down
        internal var StartPT = Point() // Record Start Position of 'img'

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val eid = event.action
            when (eid) {
                MotionEvent.ACTION_MOVE -> {
                    val mv = Point(event.x - DownPT.x, event.y - DownPT.y)
                    if (StartPT.x + mv.x + v.width.toFloat() < polygonView!!.width && StartPT.y + mv.y + v.height.toFloat() < polygonView!!.height && StartPT.x + mv.x > 0 && StartPT.y + mv.y > 0) {
                        v.x = (StartPT.x + mv.x).toInt().toFloat()
                        v.y = (StartPT.y + mv.y).toInt().toFloat()
                        StartPT = Point(v.x.toDouble(), v.y.toDouble())
                    }
                }
                MotionEvent.ACTION_DOWN -> {
                    DownPT.x = event.x.toDouble()
                    DownPT.y = event.y.toDouble()
                    StartPT = Point(v.x.toDouble(), v.y.toDouble())
                }
                MotionEvent.ACTION_UP -> {
                    var color = 0
                    if (isValidShape(points!!)) {
                        color = resources.getColor(R.color.material_deep_teal_200)
                    } else {
                        color = resources.getColor(R.color.colorAccent)
                    }
                    paint!!.color = color
                }
                else -> {
                }
            }
            polygonView!!.invalidate()
            return true
        }

    }

    private fun getBitmap(drawableRes: Int): Bitmap {
        val drawable = getResources().getDrawable(drawableRes)
        val bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888)
        return bitmap
    }
}