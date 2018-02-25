package FloodFill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

public class FloodFillAlgorithmForExteriorWalls {
	int[][] pointMatrix; // 缩放后的点,从0开始,默认单位是cm,房屋范围过大,每次的步长为1cm就太浪费了,把每次的步长设为墙厚度,这里的[0][1],和[0][2]实际相隔距离不是1cm而是墙厚度
	float xmaxInit, xminInit, ymaxInit, yminInit; // 缩放前的,墙x,y点的极值,若是弧墙,会将其缓存的点一并加入求极值
	Map<Wall, Integer> wallsTouchedCountMap = new HashMap<Wall, Integer>(); // 只储存外墙,并记录下外墙被Touched的次数
	Map<Wall, List<WallPoint>> wallsTouchedPointsMap = new HashMap<Wall, List<WallPoint>>(); // 储存外墙接触点,当找到所有的外墙后,还需要将外墙分段,砍掉外墙中延伸到房屋内部的部分.将外墙按其交点切割成多段,之后判断墙是否包含接触点,接触的则为外墙,不接触就是内墙了.这样就不用再遍历所有的点了,只遍历和外墙有关的点
	List<Wall> wallsUnTouchedList; // 为优化所设,只存储内墙,初始化为所有墙,后逐渐减去外墙,最后只剩墙分段前的所有内墙;
	int zoomtimes; // 缩放倍数,为Thickness向下取整,每次的步长就是zoomtimes
	float thickness; // 默认所有的墙厚度一致
	float halfThickness; // thickness/2
	final int borderExpend = 2; // 在边界线上外扩2个单位,保证边界不接触墙壁,保证起始点不在墙内
	Queue<FloodFillRange> ranges = new LinkedList<FloodFillRange>();

	/**
	 * Flood fill算法 弧线与弧线,弧线与直线算法还没实现 先暂缓 .四个顶点:
	 * (xmax,ymax)(xmax,ymin)(xmin,ymax)(xmin,ymin).
	 * zoomtimes:Thickness向下取整,为步长. int[][] 构建一个xy矩阵 默认为0,填充过为1 .如果墙包含当前点:
	 * 记录当前墙(注意若是当前墙的首尾包含改点(start or end),就当做该墙为包含该点),退出递归
	 * 若不包含,则记录已经走过,在pointMatrix设点为1,继续递归.
	 * 缩小尺度范围,pointMatrix是连续性整数,而访问时,是按墙厚度跳跃性访问,需要做好缩放
	 */
	public List<Wall> collectAllExteriorWalls(List<Wall> walls)
			throws Exception {
		if (walls == null || walls.size() <= 2) {
			throw new Exception("传入的墙wall:" + walls);
		}
		initParam(walls);
		floodFill(0, 0);
		// floodFill4(0, 0);
		// floodFill8(0, 0);
		List<Wall> exteriorWalls = getAllExteriorWalls();
		return exteriorWalls;
	}

	/**
	 * 初始化参数:墙厚度,缩放倍数
	 */
	public void initParam(List<Wall> walls) throws Exception {
		thickness = walls.get(0).getThickness();
		halfThickness = thickness / 2;
		if (thickness < 1) {
			throw new Exception("墙厚度太低,小于1:thickness=" + thickness);
		}
		zoomtimes = (int) thickness;
		wallsUnTouchedList = new ArrayList<Wall>(walls.size());
		for (Wall wall : walls) {
			wallsUnTouchedList.add(wall);
			wallsTouchedPointsMap.put(wall, new ArrayList<WallPoint>());
		}
		initBorderPoints(walls);
		initPointMatrix();
	}

	/**
	 * 在边界线上外扩borderExpend个单位,保证边界不接触墙壁
	 */
	public void initPointMatrix() {
		// +1:正常距离;+borderExpend上部增大;+borderExpend下部增大; 故2*borderExpend+1
		int xLength = (int) ((xmaxInit - xminInit) / zoomtimes + 2
				* borderExpend + 1);
		int ylength = (int) ((ymaxInit - yminInit) / zoomtimes + 2
				* borderExpend + 1);
		pointMatrix = new int[xLength][ylength];
		System.out.println("pointMatrix:xLength:" + xLength + " ylength:"
				+ ylength);
	}

	/**
	 * 非递归方法,以横坐标为基准,上下扩展 按行来走,每次都是计算行的范围,把每个行内的点入队 对行内所有点,出队,上下扩展,再入队
	 * 当所有点遍历完,队列为空,结束
	 * 
	 * @see https 
	 *      ://stackoverflow.com/questions/8070401/android-flood-fill-algorithm
	 *      算法复杂度 O(n)
	 * @param x
	 * @param y
	 */
	public void floodFill(int x, int y) {
		// 起始点,左右横向走
		LinearFill(x, y);
		FloodFillRange range;
		int downY, upY;
		while (ranges.size() > 0) {
			range = ranges.remove(); // 出队
			downY = range.Y - 1; // 上下扩展
			upY = range.Y + 1;

			for (int i = range.startX; i <= range.endX; i++) {
				// 边界内,没碰到墙,就记下这一行,入队
				if (upY < (pointMatrix[0].length) && (pointMatrix[i][upY] == 0)
						&& !isWallTouch(i, upY))
					LinearFill(i, upY);
				if (downY >= 0 && (pointMatrix[i][downY] == 0)
						&& !isWallTouch(i, downY))
					LinearFill(i, downY);
			}
		}
	}

	/**
	 * 左右横向走,入队
	 */
	protected void LinearFill(int x, int y) {
		// 寻找左边界点
		int leftX = x;
		while (true) {
			pointMatrix[leftX][y] = 1; // 标记已经走过
			leftX--;
			// 边界检查 是否已经走过 是否是目标点,比如是否已经触及墙壁
			if (leftX < 0 || (pointMatrix[leftX][y] == 1)
					|| isWallTouch(leftX, y)) {
				break;
			}
		}
		leftX++;

		// 寻找右边界点
		int rightX = x;
		while (true) {
			pointMatrix[rightX][y] = 1;
			rightX++;
			if (rightX >= pointMatrix.length || (pointMatrix[rightX][y] == 1)
					|| isWallTouch(rightX, y)) {
				break;
			}
		}
		rightX--;

		// 把左右点加入队列
		FloodFillRange r = new FloodFillRange(leftX, rightX, y);
		ranges.offer(r);
	}

	/**
	 * 四领域算法
	 */
	public void floodFill4(int x, int y) {
		if (!isOutBorder(x, y) && !isTouched(x, y)) {
			pointMatrix[x][y] = 1;
			if (isWallTouch(x, y)) {
				return;
			} else {
				floodFill4(x + 1, y);
				floodFill4(x - 1, y);
				floodFill4(x, y + 1);
				floodFill4(x, y - 1);
			}
		}
	}

	/**
	 * 有问题,8邻域算法一次探测的范围更大,容易把边角线添加进来,弃用
	 */
	public void floodFill8(int x, int y) {
		// if (!isOutBorder(x, y) && !isTouched(x, y)) {pointMatrix [x] [y] == 1
		if (!isOutBorder(x, y) && pointMatrix[x][y] == 0) {
			pointMatrix[x][y] = 1;
			if (isWallTouch(x, y)) {
				return;
			} else {
				floodFill8(x + 1, y);
				floodFill8(x + 1, y + 1);
				floodFill8(x + 1, y - 1);
				floodFill8(x - 1, y);
				floodFill8(x - 1, y + 1);
				floodFill8(x - 1, y - 1);
				floodFill8(x, y + 1);
				floodFill8(x, y - 1);
			}
		}
	}

	/**
	 * 初始外墙要做墙分段 有些墙壁是由内到外,需要分段,只保留要外部墙壁
	 * 
	 * @return
	 */
	private List<Wall> getAllExteriorWalls() {
		List<Wall> walls = new ArrayList<Wall>(); // 还没有做墙分段
		for (Entry<Wall, Integer> wallEntry : wallsTouchedCountMap.entrySet()) {
			if (wallEntry.getValue() != null && wallEntry.getValue() > 2) { // 一些边角可能会被算入,要触及多次才算,这里做个控制,设为2
				System.out.println(wallEntry.getKey().getName() + " ("
						+ wallEntry.getKey().getXStart() + ","
						+ wallEntry.getKey().getYStart() + "),("
						+ wallEntry.getKey().getXEnd() + ","
						+ wallEntry.getKey().getYEnd() + ")" + " count:"
						+ wallEntry.getValue());
				walls.add(wallEntry.getKey());
			}
		}
		System.out.println("AllExteriorWalls size:" + walls.size());
		System.out
				.println("----------------------记录的墙接触点-----------------------------------");
		for (Wall wall : walls) {
			System.out.println(wallsTouchedPointsMap.get(wall));
		}
		walls = getSeparatedExteriorWalls(walls); // 分段后的墙
		walls = removeInteriorWalls(walls); // 去除内墙
		addCircleWall(walls); // 圆没有考虑过,加回去
		return walls;
	}

	/**
	 * 圆还没完成,就另外处理下
	 * 
	 * @param walls
	 */
	private void addCircleWall(List<Wall> walls) {
		for (Entry<Wall, Integer> wallEntry : wallsTouchedCountMap.entrySet()) {
			if (wallEntry.getValue() != null && wallEntry.getValue() > 2) { // 一些边角可能会被算入,要触及多次才算,这里做个控制,设为2
				if (wallEntry.getKey().getArcExtent() != null) {
					walls.add(wallEntry.getKey());
				}
			}
		}
	}

	/**
	 * 要做墙分段 有些墙壁是由内到外,需要分段,只保留外部墙壁 如果有些墙和其他墙没有交点,即为没有被分段的墙,则保留该墙
	 * 
	 * @return
	 */
	private List<Wall> getSeparatedExteriorWalls(List<Wall> walls) {
		List<Wall> allExteriorWalls = new ArrayList<Wall>();
		for (int i = 0; i < walls.size(); i++) {
			// 圆先略过
			if (walls.get(i).getArcExtent() != null) {
				continue;
			}
			List<WallPoint> intersectionPoints = new ArrayList<WallPoint>();
			for (int j = 0; j < walls.size(); j++) {
				if (i == j || walls.get(j).getArcExtent() != null) {
					continue;
				}
				// 计算i,j墙的交点
				WallPoint wallPoint = getLineIntersectionPoint(walls.get(i)
						.getXStart(), walls.get(i).getYStart(), walls.get(i)
						.getXEnd(), walls.get(i).getYEnd(), walls.get(j)
						.getXStart(), walls.get(j).getYStart(), walls.get(j)
						.getXEnd(), walls.get(j).getYEnd());
				// 求出来的是直线交点,要确保点在墙上
				if (wallPoint != null
						&& walls.get(i).containsPoint(wallPoint.getX(),
								wallPoint.getY(), halfThickness)) {
					if (walls.get(j).containsPoint(wallPoint.getX(),
							wallPoint.getY(), halfThickness)) {
						// if (walls.get(j).containsWallEndAt(wallPoint.getX(),
						// wallPoint.getY(),
						// halfThickness)||walls.get(j).containsWallStartAt(wallPoint.getX(),
						// wallPoint.getY(), halfThickness)) {
						// continue;
						// }else {
						intersectionPoints.add(wallPoint);
						// }
					}

				}
			}
			List<LineSegment> iWallLineSegment = getSeparateWalls(walls.get(i),
					intersectionPoints);
			allExteriorWalls.addAll(copyWallSegment(walls.get(i),
					iWallLineSegment));
		}
		return allExteriorWalls;
	}

	/**
	 * 去除分段后的内墙 这里做了优化,由于之前就记录下所有与外墙的接触点,现在只要用记录下的接触点去判断分隔墙是否包含该点即可,不用再使用所有点取计算了
	 * 
	 * @param allWalls
	 *            :包含所有分段后的墙,也包含与其它墙没有交点的墙
	 * @return 最终线段墙集合
	 */
	private List<Wall> removeInteriorWalls(List<Wall> allWalls) {
		List<Wall> walls = new ArrayList<Wall>();
		List<WallPoint> allWallPoints = new ArrayList<WallPoint>();
		for (Entry<Wall, List<WallPoint>> entry : wallsTouchedPointsMap
				.entrySet()) {
			allWallPoints.addAll(entry.getValue());
		}
		Iterator<Wall> wallIterator = allWalls.iterator();
		while (wallIterator.hasNext()) {
			Wall wall = wallIterator.next();
			if (wallsTouchedPointsMap.containsKey(wall)) {
				walls.add(wall);
			} else {
				for (WallPoint wp : allWallPoints) {
					if (wall.containsPoint(wp.getX(), wp.getY(), halfThickness)) {
						if (wall.containsWallStartAt(wp.getX(), wp.getY(),
								halfThickness)
								|| wall.containsWallEndAt(wp.getX(), wp.getY(),
										halfThickness)) {
							continue;
						} else {
							walls.add(wall);
							break;
						}
					}
				}

			}
		}
		return walls;
	}

	/**
	 * 优化:先看当前外墙中是否已经包含该点,找不到再找非外墙
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isWallTouch(int x, int y) {
		float xindex = (x - borderExpend) * zoomtimes + xminInit;
		float yindex = (y - borderExpend) * zoomtimes + yminInit;
		boolean isWallTouch = isWallTouchTouchedCountMap(xindex, yindex);
		// 如果当前外墙中不包含改点,则找非外墙
		if (!isWallTouch) {
			isWallTouch = isWallTouchUnTouchedList(xindex, yindex);
		}
		// 记录走过改点
		if (isWallTouch) {
			pointMatrix[x][y] = 1;
		}
		return isWallTouch;
	}

	/**
	 * 看当前外墙中是否包含改点
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private boolean isWallTouchTouchedCountMap(float x, float y) {
		boolean isWallTouch = false;
		for (Entry<Wall, Integer> wallEntry : wallsTouchedCountMap.entrySet()) {
			// 墙包含点(xindex,yindex),但点不在顶点上
			if (wallEntry.getKey().containsPoint(x, y, halfThickness)) {
				if (wallEntry.getKey().containsWallStartAt(x, y, halfThickness)
						|| wallEntry.getKey().containsWallEndAt(x, y,
								halfThickness)) {
					continue;
				} else {
					wallEntry.setValue(wallEntry.getValue() + 1);
					wallsTouchedPointsMap.get(wallEntry.getKey()).add(
							new WallPoint(x, y)); // 记录下接触点
					isWallTouch = true;
					break;
				}
			}
		}
		return isWallTouch;
	}

	/**
	 * 对当前的非外墙检查是否包含点
	 * 
	 * @return
	 */
	private boolean isWallTouchUnTouchedList(float x, float y) {
		boolean isWallTouch = false;
		for (Wall wall : wallsUnTouchedList) {
			if (wall.containsPoint(x, y, halfThickness)) {
				if (wall.containsWallStartAt(x, y, halfThickness)
						|| wall.containsWallEndAt(x, y, halfThickness)) {
					continue;
				} else {
					wallsTouchedCountMap.put(wall, 1);
					wallsTouchedPointsMap.get(wall).add(new WallPoint(x, y));// 记录下接触点
					isWallTouch = true;
					break;
				}
			}
		}
		// 剔除新的外墙
		if (isWallTouch) {
			wallsUnTouchedList.removeAll(wallsTouchedCountMap.keySet());
		}
		return isWallTouch;
	}

	/**
	 * 是否在边界线外
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isOutBorder(int x, int y) {
		boolean isOutBorder = false;
		if (x < 0 || x >= (pointMatrix.length) || y < 0
				|| y >= (pointMatrix[0].length)) {
			isOutBorder = true;
		}
		return isOutBorder;
	}

	/**
	 * 是否已经走过
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isTouched(int x, int y) {
		boolean isTouched = false;
		if (pointMatrix[x][y] == 1) {
			isTouched = true;
		}
		return isTouched;
	}

	/**
	 * 考虑到有弯曲的墙,把cachePoint也算入进去 初始化xmaxInit, xminInit, ymaxInit, yminInit
	 * 
	 * @param walls
	 * @return xmin,xmax,ymin,ymax
	 */
	public void initBorderPoints(List<Wall> walls) {
		List<Float> xpoints = new ArrayList<Float>(walls.size());
		List<Float> ypoints = new ArrayList<Float>(walls.size());
		for (Wall wall : walls) {
			xpoints.add(wall.getXStart());
			xpoints.add(wall.getXEnd());
			ypoints.add(wall.getYStart());
			ypoints.add(wall.getYEnd());
			// 墙弯曲
			if (wall.getArcExtent() != null) {
				float[][] cachePoints = wall.getPoints();
				for (float[] fs : cachePoints) {
					xpoints.add(fs[0]);
					ypoints.add(fs[1]);
				}
			}
		}
		float[] xminMax = getMinMax(xpoints);
		float[] yminMax = getMinMax(ypoints);
		xminInit = xminMax[0];
		xmaxInit = xminMax[1];
		yminInit = yminMax[0];
		ymaxInit = yminMax[1];
		Collections.sort(xpoints);
		Collections.sort(ypoints);
		System.out.println("xpoints:" + xpoints);
		System.out.println("ypoints:" + ypoints);
		System.out.println("xminInit:" + xminInit);
		System.out.println("xmaxInit:" + xmaxInit);
		System.out.println("yminInit:" + yminInit);
		System.out.println("ymaxInit:" + ymaxInit);
	}

	/**
	 * @return float[0]:min float[1]:max
	 */
	public float[] getMinMax(List<Float> values) {
		float[] minMaxValue = new float[2];
		minMaxValue[0] = minMaxValue[1] = values.get(0);
		for (float f : values) {
			if (minMaxValue[0] > f) {
				minMaxValue[0] = f;
			}
			if (minMaxValue[1] < f) {
				minMaxValue[1] = f;
			}
		}
		return minMaxValue;
	}

	/**
	 * 按交点给墙分段,循环分段 取出所有两点间线段,每次取出最短的(即目标线段),再删除包含最短的线段
	 * 
	 * @param wall
	 * @param wallPoints
	 * @return
	 */
	public List<LineSegment> getSeparateWalls(Wall wall,
			List<WallPoint> wallPoints) {
		List<LineSegment> lineSegments = new ArrayList<LineSegment>();
		WallPoint wallPointStart = new WallPoint(wall.getXStart(),
				wall.getYStart());
		WallPoint wallPointEnd = new WallPoint(wall.getXEnd(), wall.getYEnd());
		// 只有一个点,直接返回
		if (wallPoints.size() == 1) {
			lineSegments
					.add(new LineSegment(wallPointStart, wallPoints.get(0)));
			lineSegments.add(new LineSegment(wallPoints.get(0), wallPointEnd));
			return lineSegments;
		}
		List<LineSegment> lineSegmentsTemp = new LinkedList<LineSegment>();
		List<WallPoint> wallPointsTemp = new ArrayList<WallPoint>(wallPoints);
		addExtremePoint(wallPointsTemp, wallPointStart);
		addExtremePoint(wallPointsTemp, wallPointEnd);
		// 每两个点都取线段
		for (int i = 0; i < wallPointsTemp.size(); i++) {
			for (int j = i + 1; j < wallPointsTemp.size(); j++) {
				lineSegmentsTemp.add(new LineSegment(wallPointsTemp.get(i),
						wallPointsTemp.get(j)));
			}
		}
		Collections.sort(lineSegmentsTemp); // 链式一次排好序了,以后不用再排
		/**
		 * 段数=点数-1 循环读取,每次读取长度最小的,读完后立刻删除包含最小的线段
		 */
		while (lineSegments.size() < wallPointsTemp.size() - 1) {
			LineSegment shortestLine = lineSegmentsTemp.get(0);
			lineSegments.add(shortestLine);
			lineSegmentsTemp.remove(shortestLine);
			removeInvalidLineSegments(lineSegmentsTemp, shortestLine);
		}
		return lineSegments;
	}

	/**
	 * 把线段端点加上去
	 * 
	 * @param points
	 * @param p
	 * @return
	 */
	private void addExtremePoint(List<WallPoint> points, WallPoint wallPoint) {
		Iterator<WallPoint> wpIterator = points.iterator();
		boolean isExits = false;
		while (wpIterator.hasNext()) {
			WallPoint wp = wpIterator.next();
			if (isZero(wp.getX() - wallPoint.getX())
					&& isZero(wp.getY() - wallPoint.getY())) {
				isExits = true;
				break;
			}
		}
		if (!isExits) {
			points.add(wallPoint);
		}
	}

	/**
	 * 没有交点就返回当前墙壁
	 * 
	 * @param wall
	 * @param lineSegments
	 * @return
	 */
	private List<Wall> copyWallSegment(Wall wall, List<LineSegment> lineSegments) {
		List<Wall> walls = new ArrayList<Wall>();
		// 没有交点就返回当前墙壁
		if (lineSegments.size() == 0) {
			walls.add(wall);
		} else {
			for (LineSegment lineSegment : lineSegments) {
				Wall wallSegment = wall.clone();
				wallSegment.setXStart(lineSegment.getStartPoint().getX());
				wallSegment.setYStart(lineSegment.getStartPoint().getY());
				wallSegment.setXEnd(lineSegment.getEndPoint().getX());
				wallSegment.setYEnd(lineSegment.getEndPoint().getY());
				walls.add(wallSegment);
			}
		}
		return walls;
	}

	/**
	 * 删除无效的线段 即删除包含shortLine或与shortLine相交的所有线段
	 * 
	 * @param lineSegments
	 * @param shortLine
	 */
	public void removeInvalidLineSegments(List<LineSegment> lineSegments,
			LineSegment shortLine) {
		WallPoint stdPoint1 = shortLine.getStartPoint();
		WallPoint stdPoint2 = shortLine.getEndPoint();
		Iterator<LineSegment> iterator = lineSegments.iterator();
		while (iterator.hasNext()) {
			LineSegment ls = iterator.next();
			WallPoint p1 = ls.getStartPoint();
			WallPoint p2 = ls.getEndPoint();
			WallPoint bp; // bigger
			WallPoint lp; // less
			if (p1.getX() != p2.getX()) {
				if (p1.getX() > p2.getX()) {
					bp = p1;
					lp = p2;
				} else {
					bp = p2;
					lp = p1;
				}
				if (isPointInLineSegment(stdPoint1.getX(), lp.getX(), bp.getX())
						|| isPointInLineSegment(stdPoint2.getX(), lp.getX(),
								bp.getX())) {
					iterator.remove();
				}
			} else {
				if (p1.getY() > p2.getY()) {
					bp = p1;
					lp = p2;
				} else {
					bp = p2;
					lp = p1;
				}
				if (isPointInLineSegment(stdPoint1.getY(), lp.getY(), bp.getY())
						|| isPointInLineSegment(stdPoint2.getY(), lp.getY(),
								bp.getY())) {
					iterator.remove();
				}
			}
		}
	}

	/**
	 * 判断值是否在范围内
	 * 
	 * @return
	 */
	private boolean isPointInLineSegment(float stdPointValue,
			float lineSegmentStart, float lineSegmentEnd) {
		if (stdPointValue > lineSegmentStart && stdPointValue < lineSegmentEnd) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * @return 求直线交点
	 */
	public WallPoint getLineIntersectionPoint(float line1Startx,
			float line1Starty, float line1endx, float line1endy,
			float line2Startx, float line2Starty, float line2endx,
			float line2endy) {
		WallPoint wallPoint1start = new WallPoint(line1Startx, line1Starty);
		WallPoint wallPoint1end = new WallPoint(line1endx, line1endy);
		WallPoint wallPoint2start = new WallPoint(line2Startx, line2Starty);
		WallPoint wallPoint2end = new WallPoint(line2endx, line2endy);
		LinearEquation e1 = new LinearEquation(wallPoint1start, wallPoint1end);
		LinearEquation e2 = new LinearEquation(wallPoint2start, wallPoint2end);
		return getLineIntersectionPoint(e1, e2);
	}

	/**
	 * 求两直线交点 a1x+b1=y a2x+b2=y x = (b1-b2)/(a2-a1) 注意平行线和 竖线
	 * 
	 * @return
	 */
	public WallPoint getLineIntersectionPoint(LinearEquation e1,
			LinearEquation e2) {
		WallPoint wallPoint = null;

		if (isZero(e1.getA() - e2.getA())) {// 平行或重叠和 标准垂直-|线
			String message = "平行线";
			if (isZero(e1.getB() - e2.getB())) { // 重叠
				message = "重叠线";
			}
			if (e1.isVerticalLine() && !e2.isVerticalLine()
					&& isZero(e2.getA())) { // e1:|
											// e2:-
				wallPoint = new WallPoint(e1.getStartPoint().getX(), e2
						.getStartPoint().getY());
				message = "标准垂直线-|";
			} else if (e2.isVerticalLine() && !e1.isVerticalLine()
					&& isZero(e1.getA())) { // e2:|
											// e1:-
				wallPoint = new WallPoint(e2.getStartPoint().getX(), e1
						.getStartPoint().getY());
				message = "标准垂直线-|";
			}
			System.out.println(message + e1 + " " + e2 + wallPoint);
			return wallPoint;
		}
		float x = (e1.getB() - e2.getB()) / (e2.getA() - e1.getA());
		float y;
		if (e1.isVerticalLine()) {
			x = e1.getStartPoint().getX();
			y = e2.getA() * x + e2.getB();
		} else if (e2.isVerticalLine()) {
			x = e2.getStartPoint().getX();
			y = e1.getA() * x + e1.getB();
		} else {
			y = e1.getA() * x + e1.getB();
		}
		wallPoint = new WallPoint(x, y);
		System.out.println("直线:" + e1 + " " + e2 + " 的交点是:" + wallPoint);
		return wallPoint;
	}

	private boolean isZero(float num) {
		if (Math.abs(num) < 0.01) {
			return true;
		}
		return false;
	}
}

/**
 * 线段
 */
class LineSegment implements Comparable<LineSegment> {
	private WallPoint startPoint, endPoint;
	private double length = -1;

	/**
	 * 线段
	 */
	public LineSegment(WallPoint startPoint, WallPoint endPoint) {
		super();
		this.startPoint = startPoint;
		this.endPoint = endPoint;
		calLength();
	}

	/**
	 * ( (y2-y1)^2+(x2-x1)^2 )^0.5
	 */
	private void calLength() {
		double ytemp = Math.pow(endPoint.getY() - startPoint.getY(), 2);// (y2-y1)^2
		double xtemp = Math.pow(endPoint.getX() - startPoint.getX(), 2);// (x2-x1)^2
		this.length = Math.pow((ytemp + xtemp), 0.5);
	}

	public WallPoint getStartPoint() {
		return this.startPoint;
	}

	public WallPoint getEndPoint() {
		return this.endPoint;
	}

	public double getLength() {
		// calLength();
		return this.length;
	}

	public int compareTo(LineSegment o) {
		if (this.length > o.length) {
			return 1;
		} else if (this.length < o.length) {
			return -1;
		}
		return 0;
	}

	@Override
	public String toString() {
		return "LineSegment [startPoint=" + this.startPoint + ", endPoint="
				+ this.endPoint + ", length=" + this.length + "]";
	}
}

/**
 * 直线方程 y=ax+b
 */
class LinearEquation {

	private float a = 0, b = 0;
	private boolean isVerticalLine = false; // 是否垂直线
	private WallPoint startPoint, endPoint; // 处理垂线时需要

	/**
	 * 直线方程 y=ax+b a = (y2-y1)/(x2-x1) b=y-ax
	 * 
	 * @param wallPointStart
	 * @param wallPointEnd
	 */
	public LinearEquation(WallPoint wallPointStart, WallPoint wallPointEnd) {
		this.startPoint = wallPointStart;
		this.endPoint = wallPointEnd;
		if ((Math.abs(wallPointEnd.getX() - wallPointStart.getX())) < 0.01) {
			this.isVerticalLine = true;
		} else {
			a = (wallPointEnd.getY() - wallPointStart.getY())
					/ (wallPointEnd.getX() - wallPointStart.getX());
			b = wallPointEnd.getY() - a * wallPointEnd.getX();
		}
	}

	public float getA() {
		return this.a;
	}

	public float getB() {
		return this.b;
	}

	@Override
	public String toString() {
		return "y=" + a + "x＋" + b;
	}

	/**
	 * 是否是垂直线
	 * 
	 * @return Returns the isVerticalLine.
	 */
	public boolean isVerticalLine() {
		return isVerticalLine;
	}

	public WallPoint getStartPoint() {
		return this.startPoint;
	}

	public WallPoint getEndPoint() {
		return this.endPoint;
	}
}

class WallPoint {
	private float x;
	private float y;

	public WallPoint(float x, float y) {
		super();
		this.x = x;
		this.y = y;
	}

	public float getX() {
		return this.x;
	}

	public float getY() {
		return this.y;
	}

	@Override
	public String toString() {
		return " (" + this.x + "," + this.y + ")";
	}

}

/**
 * 辅助类,记录下在pointMatrix中,(startX,Y)到(endX,Y)的点都不在墙内
 */
class FloodFillRange {
	public int startX;
	public int endX;
	public int Y;

	public FloodFillRange(int startX, int endX, int y) {
		this.startX = startX;
		this.endX = endX;
		this.Y = y;
	}

	@Override
	public String toString() {
		return "FloodFillRange [startX=" + this.startX + ", endX=" + this.endX
				+ ", Y=" + this.Y + "]";
	}
}
