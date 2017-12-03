package ppapps.cropreceiptdemo.opencv

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.utils.Converters
import ppapps.cropreceiptdemo.cropreceipt.PolygonView
import java.util.*

/**
 * Created by phuchoang on 11/27/17
 */
class OpenCVUtils {
    companion object {
        fun getContourEdgePoints(bitmap: Bitmap): List<Point>? {
            var hasContour = false
            var matReceipt = Mat()
            matReceipt = convertBitmapToMat(bitmap)
            compressDown(matReceipt, matReceipt)
            compressDown(matReceipt, matReceipt)

            // Resize and convert to grayscale
            val matConvertedGray = Mat()
            Imgproc.cvtColor(matReceipt, matConvertedGray, Imgproc.COLOR_BGR2GRAY)
//        Bitmap ex1 = convertMatToBitmap(matConvertedGray);

            // Get threshold for helping Canny method do more exactly
            val otsuThresold = Imgproc.threshold(matConvertedGray, Mat(), 0.0, 255.0, Imgproc.THRESH_OTSU)
//        Bitmap ex21 = convertMatToBitmap(bw);

            // Reduce noise
            val matMedianFilter = Mat()
            Imgproc.medianBlur(matConvertedGray, matMedianFilter, 11)
//        Bitmap ex4 = convertMatToBitmap(medianFilter);

            // Draw receipt with only lines
            val matEdges = Mat()
            Imgproc.Canny(matConvertedGray, matEdges, otsuThresold * 0.05, otsuThresold)
//        Bitmap ex6 = convertMatToBitmap(edges);

            // Find contour of Object
            val contours = ArrayList<MatOfPoint>()
            Imgproc.findContours(matEdges, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            val height = matConvertedGray.height()
            val width = matConvertedGray.width()

            //Initial maxAreaFound. The variable is used to find max area of contour
            var maxAreaFound = ((width - 20) * (height - 20) / 20).toDouble()

            val myPoints = arrayOf(Point(5.0, 5.0), Point(5.0, (height - 5).toDouble()), Point((width - 5).toDouble(), (height - 5).toDouble()), Point((width - 5).toDouble(), 5.0))
            var receiptContour = MatOfPoint(*myPoints)

            for (i in contours.indices) {
                // Simplify contour
                val contour = contours[i]

                val boxContour2F = MatOfPoint2f(*contour.toArray())
                val boxArea = Imgproc.minAreaRect(boxContour2F)


                if (contour.toArray().size >= 4 && maxAreaFound < boxArea.size.area()) {
                    maxAreaFound = boxArea.size.area()
                    receiptContour = contour

                    //Convex Hull to convert any shape of contour into convex Hull
                    val hull = MatOfInt()
                    Imgproc.convexHull(receiptContour, hull, false)
                    val mopOut = MatOfPoint()
                    mopOut.create(hull.size().height.toInt(), 1, CvType.CV_32SC2)
                    var j = 0
                    while (j < hull.size().height) {
                        val index = hull.get(j, 0)[0].toInt()
                        val point = doubleArrayOf(receiptContour.get(index, 0)[0], receiptContour.get(index, 0)[1])
                        mopOut.put(j, 0, *point)
                        j++
                    }
                    receiptContour = mopOut

                    hasContour = true

                    //                List<MatOfPoint> contours1 = new ArrayList<>();
                    //                contours1.add(mopOut);
                    //                Scalar color = new Scalar(216, 112, 112);
                    //                Imgproc.drawContours(matReceipt, contours1, 0, color, 2, 8, new Mat(), 0, new Point());
                    //                Bitmap ex24 = convertMatToBitmap(matReceipt);
                    //                Log.d("ABC", "ABC");
                }
            }

            // Use moments to find centroid of convex
            val centrePoint = getCentrePointOfContour(receiptContour)
//        Core.circle(matReceipt, centrePoint, (int) 2, new Scalar(112, 112, 112), 2);
//        Bitmap ex24 = convertMatToBitmap(matReceipt);
            // These variable to help find corner of skew receipt more exactly
            val listPoints = receiptContour.toList()
            if (listPoints == null || listPoints.size < 4) {
                return listPoints
            }
            val pMaxX = getMaxX(listPoints)
            val pMinX = getMinX(listPoints)
            val espX = (pMaxX - pMinX) / 4

            val pMinY = getMinY(listPoints)
            val pMaxY = getMaxY(listPoints)
            val espY = (pMaxY - pMinY) / 4

            var pointTL = getPointTlWithMaxLength(listPoints, centrePoint!!, espX, espY)
            var pointTR = getPointTrWithMaxLength(listPoints, centrePoint!!, espX, espY)
            var pointBR = getPointBrWithMaxLength(listPoints, centrePoint!!, espX, espY)
            var pointBL = getPointBlWithMaxLength(listPoints, centrePoint!!, espX, espY)

            val cornerPoints = ArrayList<Point>()
            cornerPoints.add(pointTL!!)
            cornerPoints.add(pointTR!!)
            cornerPoints.add(pointBR!!)
            cornerPoints.add(pointBL!!)

            if (!isConvexShape(cornerPoints)) {
                val box = Imgproc.boundingRect(receiptContour)
                cornerPoints.clear()
                pointTL = box.tl()
                pointBR = box.br()
                pointTR = Point(pointBR.x, pointTL.y)
                pointBL = Point(pointTL.x, pointBR.y)
                cornerPoints.add(pointTL)
                cornerPoints.add(pointTR)
                cornerPoints.add(pointBR)
                cornerPoints.add(pointBL)
            }

//        for (int i = 0; i < cornerPoints.size(); i++) {
//            Core.circle(matReceipt, cornerPoints.get(i), (int) 2, new Scalar(112, 112, 112), 2);
//        }
//        Bitmap ex241 = convertMatToBitmap(matReceipt);

            if (hasContour) {
                val pyrDownReceipt = convertMatToBitmap(matReceipt)
                val widthRatio = bitmap.getWidth().toDouble() / pyrDownReceipt.width.toDouble()
                val heightRatio = bitmap.getHeight().toDouble() / pyrDownReceipt.height.toDouble()

                val convertedCorners = ArrayList<Point>()
                for (corner in cornerPoints) {
                    convertedCorners.add(Point(corner.x * widthRatio, corner.y * heightRatio))
                }
                return convertedCorners
            } else {
                return listPoints
            }
        }

        fun getEdgePoints(bitmap: Bitmap, polygonView: PolygonView): Map<Int, Point>? {
            val pointFs = getContourEdgePoints(bitmap)
            return orderedValidEdgePoints(bitmap, pointFs, polygonView)
        }

        private fun getOutlinePoints(bitmap: Bitmap): Map<Int, Point> {
            val outlinePoints = HashMap<Int, Point>()
            outlinePoints.put(0, Point(0f.toDouble(), 0f.toDouble()))
            outlinePoints.put(1, Point(bitmap.width.toDouble(), 0f.toDouble()))
            outlinePoints.put(2, Point(0f.toDouble(), bitmap.height.toDouble()))
            outlinePoints.put(3, Point(bitmap.width.toDouble(), bitmap.height.toDouble()))
            return outlinePoints
        }

        private fun orderedValidEdgePoints(bitmap: Bitmap, pointFs: List<Point>?, polygonView: PolygonView): Map<Int, Point> {
            var orderedPoints = polygonView.getOrderedPoints(pointFs)
            if (!polygonView.isValidShape(orderedPoints!!)) {
                orderedPoints = getOutlinePoints(bitmap)
            }
            return orderedPoints
        }

        private fun isScanPointsValid(points: Map<Int, PointF>): Boolean {
            return points.size == 4
        }

        private fun scaledBitmap(bitmap: Bitmap?, width: Int, height: Int): Bitmap {
            val m = Matrix()
            m.setRectToRect(RectF(0f, 0f, bitmap!!.width.toFloat(), bitmap.height.toFloat()), RectF(0f, 0f, width.toFloat(), height.toFloat()), Matrix.ScaleToFit.CENTER)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        }

        fun getMaxX(pointList: List<Point>?): Double {
            if (pointList == null || pointList.size == 0) {
                return 0.0
            }
            var pos = 0
            var maxX = pointList[0].x
            for (i in pointList.indices) {
                if (maxX < pointList[i].x) {
                    pos = i
                    maxX = pointList[i].x
                }
            }
            return pointList[pos].x
        }

        fun getMinX(pointList: List<Point>?): Double {
            if (pointList == null || pointList.size == 0) {
                return 0.0
            }
            var pos = 0
            var minX = pointList[0].x
            for (i in pointList.indices) {
                if (minX >= pointList[i].x) {
                    pos = i
                    minX = pointList[i].x
                }
            }
            return pointList[pos].x
        }

        fun getMinY(pointList: List<Point>?): Double {
            if (pointList == null || pointList.size == 0) {
                return 0.0
            }
            var pos = 0
            var minY = pointList[0].y
            for (i in pointList.indices) {
                if (minY >= pointList[i].y) {
                    pos = i
                    minY = pointList[i].y
                }
            }
            return pointList[pos].y
        }

        fun getMaxY(pointList: List<Point>?): Double {
            if (pointList == null || pointList.size == 0) {
                return 0.0
            }
            var pos = 0
            var maxY = pointList[0].y
            for (i in pointList.indices) {
                if (maxY < pointList[i].y) {
                    pos = i
                    maxY = pointList[i].y
                }
            }
            return pointList[pos].y
        }

        fun getPointTlWithMaxLength(listPointInContour: List<Point>?, centrePoint: Point, espX: Double, espY: Double): Point? {
            if (listPointInContour == null || listPointInContour.size == 0)
                return null
            //        Point centrePoint = getCentrePoint(listPointInContour);
            var maxLength = 0.0
            var pos = 0
            for (i in listPointInContour.indices) {
                val point = listPointInContour[i]
                val length = getDistanceBetweenPoints(point, centrePoint)
                if (point.x <= centrePoint.x + espX && point.y <= centrePoint.y - espY && maxLength < length) {
                    pos = i
                    maxLength = length
                }
            }
            return listPointInContour[pos]
        }

        fun getPointTrWithMaxLength(listPointInContour: List<Point>?, centrePoint: Point, espX: Double, espY: Double): Point? {
            if (listPointInContour == null || listPointInContour.size == 0)
                return null
            //        Point centrePoint = getCentrePoint(listPointInContour);
            var maxLength = 0.0
            var pos = 0
            for (i in listPointInContour.indices) {
                val point = listPointInContour[i]
                val length = getDistanceBetweenPoints(point, centrePoint)
                if (point.x > centrePoint.x + espX && point.y <= centrePoint.y + espY && maxLength < length) {
                    pos = i
                    maxLength = length
                }
            }
            return listPointInContour[pos]
        }

        fun getPointBrWithMaxLength(listPointInContour: List<Point>?, centrePoint: Point, espX: Double, espY: Double): Point? {
            if (listPointInContour == null || listPointInContour.size == 0)
                return null
            //        Point centrePoint = getCentrePoint(listPointInContour);
            var maxLength = 0.0
            var pos = 0
            for (i in listPointInContour.indices) {
                val point = listPointInContour[i]
                val length = getDistanceBetweenPoints(point, centrePoint)
                if (point.x > centrePoint.x - espX && point.y > centrePoint.y + espY && maxLength < length) {
                    pos = i
                    maxLength = length
                }
            }
            return listPointInContour[pos]
        }

        fun getPointBlWithMaxLength(listPointInContour: List<Point>?, centrePoint: Point, espX: Double, espY: Double): Point? {
            if (listPointInContour == null || listPointInContour.size == 0)
                return null
            //        Point centrePoint = getCentrePoint(listPointInContour);
            var maxLength = 0.0
            var pos = 0
            for (i in listPointInContour.indices) {
                val point = listPointInContour[i]
                val length = getDistanceBetweenPoints(point, centrePoint)
                if (point.x <= centrePoint.x - espX && point.y > centrePoint.y - espY && maxLength < length) {
                    pos = i
                    maxLength = length
                }
            }
            return listPointInContour[pos]
        }

        fun getDistanceBetweenPoints(point1: Point, point2: Point): Double {
            return Math.sqrt((point1.x - point2.x) * (point1.x - point2.x) + (point1.y - point2.y) * (point1.y - point2.y))
        }

        private fun isConvexShape(corners: List<Point>?): Boolean {
            var size = 0
            var result = false

            if (corners == null || corners.isEmpty()) {
                return false
            } else {
                size = corners.size
            }
            if (size > 0) {
                for (i in 0 until size) {
                    val dx1 = corners[(i + 2) % size].x - corners[(i + 1) % size].x
                    val dy1 = corners[(i + 2) % size].y - corners[(i + 1) % size].y
                    val dx2 = corners[i].x - corners[(i + 1) % size].x
                    val dy2 = corners[i].y - corners[(i + 1) % size].y
                    val crossProduct = dx1 * dy2 - dy1 * dx2
                    if (i == 0) {
                        result = crossProduct > 0
                    } else {
                        if (result != crossProduct > 0) {
                            return false
                        }
                    }
                }
                return true
            } else {
                return false
            }
        }

        fun getCentrePointOfContour(contour: MatOfPoint): Point? {
            val moments = Imgproc.moments(contour)
            return if (moments != null) {
                Point(moments._m10 / moments._m00, moments._m01 / moments._m00)
            } else {
                null
            }
        }

        private fun compressDown(large: Mat, rgb: Mat) {
            Imgproc.pyrDown(large, rgb)
            Imgproc.pyrDown(rgb, rgb)
        }


        private fun convertBitmapToMat(bitmap: Bitmap): Mat {
            val mat = Mat(bitmap.width, bitmap.height, CvType.CV_8UC1)
            val bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            Utils.bitmapToMat(bmp32, mat)
            return mat
        }

        private fun convertMatToBitmap(m: Mat): Bitmap {
            val bm = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(m, bm)
            return bm
        }

        fun rotate(bitmap: Bitmap, degree: Int): Bitmap {
            try {
                val w = bitmap.width
                val h = bitmap.height
                val mtx = Matrix()
                mtx.postRotate(degree.toFloat())
                return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false)
            } catch (e: Exception) {
                return bitmap
            }

        }

        fun cropReceiptByFourPoints(receipt: Bitmap, cornerPoints: ArrayList<Point>?, screenWidth: Int, screenHeight: Int): Bitmap? {
            if (cornerPoints == null || cornerPoints.size != 4) {
                return null
            }
            val originalReceiptMat = convertBitmapToMat(receipt)

            val widthRatio = receipt.width.toDouble() / screenWidth.toDouble()
            val heightRatio = receipt.height.toDouble() / screenHeight.toDouble()

            val corners = ArrayList<Point>()
            for (i in cornerPoints.indices) {
                corners.add(Point(cornerPoints[i].x * widthRatio, cornerPoints[i].y * heightRatio))
            }

            //        for (Point corner : corners) {
            //            Core.circle(originalReceiptMat, corner, (int) 20, new Scalar(216, 162, 162), 20);
            //        }
            //        Bitmap temp = convertMatToBitmap(originalReceiptMat);

            //2
            val srcPoints = Converters.vector_Point2f_to_Mat(corners)
            val maxY = getPointWithMaxCorY(corners)!!.y
            val minY = getPointWithMinCorY(corners)!!.y
            val maxX = getPointWithMaxCorX(corners)!!.x
            val minX = getPointWithMinCorX(corners)!!.x

            val maxWidth = maxX - minX
            val maxHeight = maxY - minY

            val correctedImage = Mat(maxHeight.toInt(), maxWidth.toInt(), originalReceiptMat.type())

            val destPoints = Converters.vector_Point2f_to_Mat(Arrays.asList(Point(0.0, 0.0),
                    Point(maxWidth - 1, 0.0),
                    Point(maxWidth - 1, maxHeight - 1),
                    Point(0.0, maxHeight - 1)))

            val transformation = Imgproc.getPerspectiveTransform(srcPoints,
                    destPoints)

            Imgproc.warpPerspective(originalReceiptMat, correctedImage, transformation,
                    correctedImage.size())
            return convertMatToBitmap(correctedImage)
        }

        fun getPointWithMaxCorY(listPoint: List<Point>?): Point? {
            if (listPoint == null || listPoint.size == 0) {
                return null
            }
            var maxY = listPoint[0].y
            var maxYPos = 0
            for (i in listPoint.indices) {
                if (maxY < listPoint[i].y) {
                    maxY = listPoint[i].y
                    maxYPos = i
                }
            }
            return listPoint[maxYPos]
        }

        fun getPointWithMinCorY(listPoint: List<Point>?): Point? {
            if (listPoint == null || listPoint.size == 0) {
                return null
            }
            if (listPoint == null || listPoint.size == 0) {
                return null
            }
            var minY = listPoint[0].y
            var minYPos = 0
            for (i in listPoint.indices) {
                if (minY > listPoint[i].y) {
                    minY = listPoint[i].y
                    minYPos = i
                }
            }
            return listPoint[minYPos]
        }

        fun getPointWithMaxCorX(listPoint: List<Point>?): Point? {
            if (listPoint == null || listPoint.size == 0) {
                return null
            }
            var maxX = listPoint[0].x
            var maxXPos = 0
            for (i in listPoint.indices) {
                if (maxX < listPoint[i].x) {
                    maxX = listPoint[i].x
                    maxXPos = i
                }
            }
            return listPoint[maxXPos]
        }

        fun getPointWithMinCorX(listPoint: List<Point>?): Point? {
            if (listPoint == null || listPoint.size == 0) {
                return null
            }
            var minX = listPoint[0].x
            var minXPos = 0
            for (i in listPoint.indices) {
                if (minX > listPoint[i].x) {
                    minX = listPoint[i].x
                    minXPos = i
                }
            }
            return listPoint[minXPos]
        }

    }

}