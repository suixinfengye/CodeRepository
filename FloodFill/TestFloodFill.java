package FloodFill;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.text.ViewFactory;

public class TestFloodFill extends ComponentTestFixture {
  final UserPreferences preferences = new DefaultUserPreferences();                            // 用户参数
  Home                  home        = new Home();
  ViewFactory           viewFactory = new SwingViewFactory();
  final HomeController  controller  = new HomeController(home, preferences, viewFactory, null);
  final JComponent      homeView    = (JComponent)controller.getView();

  protected void setUp() throws Exception {
    super.setUp();
    preferences.setUnit(LengthUnit.CENTIMETER);
    // Create a frame that displays a home view
    JFrame frame = new JFrame("Background Image Wizard Test");
    frame.add(homeView);
    frame.pack();

    // Show home plan frame
    showWindow(frame);
  }

  public void testHomeFurniturePanel() {
    addWalls();
    //这里打断点,在软件上要先选中整个模型,否则selectList为null
    List<Selectable> selectList = home.getSelectedItems();
    List<Wall> walls = getWalls(selectList);
    FloodFillAlgorithmForExteriorWalls algorithm = new FloodFillAlgorithmForExteriorWalls();
    try {
      Date startDate = new Date();
      List<Wall> exteriorWalls = algorithm.collectAllExteriorWalls(walls);
      removeInteriorWalls(walls, exteriorWalls);
      Date endDate = new Date();
      System.out.println("计算时间:" + (endDate.getTime() - startDate.getTime()) / 1000.0);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private void removeInteriorWalls(List<Wall> allWalls, List<Wall> exteriorWalls) {
    for (Wall wall : allWalls) {
      home.deleteWall(wall);
    }
    for (Wall wall : exteriorWalls) {
      home.addWall(wall);
    }
  }

  private void addWalls() {
    Wall upWall = new Wall(0, 0, 600, 0, 7, home.getWallHeight());
    Wall upWall2 = new Wall(600, 0, 1000, 0, 7, home.getWallHeight());
    upWall2.setArcExtent(1.3f);
    Wall downWall = new Wall(0, 500, 1000, 500, 7, home.getWallHeight());
    Wall leftall = new Wall(0, 0, 0, 500, 7, home.getWallHeight());
    Wall rightWall = new Wall(1000, 0, 1000, 500, 7, home.getWallHeight());
    Wall leftCrossWall1 = new Wall(0, 100, -100, 200, 7, home.getWallHeight());
    Wall leftCrossWall2 = new Wall(0, 300, -100, 200, 7, home.getWallHeight());
    Wall rightCrossWall1 = new Wall(1000, 200, 1200, 300, 7, home.getWallHeight());
    Wall rightCrossWal2 = new Wall(1000, 400, 1200, 300, 7, home.getWallHeight());
    Wall insideWall = new Wall(600, 0, 600, 500, 7, home.getWallHeight());
    Wall insideWall1 = new Wall(0, 200, 600, 200, 7, home.getWallHeight());
    Wall insideWall2 = new Wall(600, 200, 1000, 200, 7, home.getWallHeight());
    Wall crossWall1 = new Wall(400, 700, 300, 450, 7, home.getWallHeight());
    Wall crossWall2 = new Wall(400, 700, 200, 500, 7, home.getWallHeight());
    insideWall2.setArcExtent(0.8f);
    List<Wall> walls = new ArrayList<Wall>();
    walls.add(upWall);
    walls.add(upWall2);
    walls.add(downWall);
    walls.add(leftall);
    walls.add(rightWall);
    walls.add(leftCrossWall1);
    walls.add(leftCrossWall2);
    walls.add(rightCrossWal2);
    walls.add(rightCrossWall1);
    walls.add(insideWall);
    walls.add(insideWall1);
    walls.add(insideWall2);
    walls.add(crossWall1);
    walls.add(crossWall2);
    for (Wall wall : walls) {
      home.addWall(wall);
    }
  }

  private List<Wall> getWalls(List<Selectable> selectList) {
    List<Wall> walls = new ArrayList<Wall>();
    for (Selectable item : selectList) {
      if (item instanceof Wall) {
        walls.add((Wall)item);
      }
    }
    return walls;
  }
}