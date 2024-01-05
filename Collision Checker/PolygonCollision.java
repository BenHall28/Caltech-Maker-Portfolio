import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.TreeSet;

public class PolygonCollision {
   
   /**
    * Checks for an intersection/overlap between the two polygons using an algorithm I developed by modifying a line 
    *    sweep intersection test for line segments from <a href="https://www.geeksforgeeks.org/given-a-set-of-line-segments-find-if-any-two-segments-intersect">Geeks for Geeks</a>.         <br>
    * There are two conditions which would indicate an intersection:                                                                                                                         <p style="margin-left: 25px;">
    *    A: One shape contains any or all vertices of the other. This could be when one shape fully contains another, 
    *       or just when a shape intersects another in a certain way                                                                                                                         <br>
    *
    *    B: At least one pair of two line segments from the polygons (one segment from each polygon) intersect. 
    *       This will be true whenever the shapes intersect, but one does not entirely contain the other, but will 
    *       be slower to evaluate than case A.
    *
    */
   public static boolean twoPolygonCollision(Polygon p, Polygon p1) {
            
      //Checks for case A
      for(int i = 0; i < p.npoints; i++){
         if(p1.contains(p.xpoints[i], p.ypoints[i])) {
            return true;
         }
      }
      for(int i = 0; i < p1.npoints; i++) {
         if (p.contains(p1.xpoints[i], p1.ypoints[i])) {
            return true;
         }
      }
      
      //Converts the two polygons into a list of PolygonPoints/line segments
      PriorityQueue<PolygonPoint> stored = new PriorityQueue<PolygonPoint>(4, Comparator.comparingInt((PolygonPoint o) -> o.x));
      sortIn(stored, p, 0);
      sortIn(stored, p1, 1);
      
      //Check for case B
      return checkCollisions(stored);
   }
   
   /**
    * Checks for the collision of any two lines out of a set denoted by polygon points. 
    * This is done using a modified version of <a href="https://www.geeksforgeeks.org/given-a-set-of-line-segments-find-if-any-two-segments-intersect">an algorithm from Geeks for Geeks</a>. 
    * This version is modified to ignore collisions from line segments from the same shape, 
    * to work with polygon vertices, and for slight performance improvements.
    */
   private static boolean checkCollisions(PriorityQueue<PolygonPoint> stored){
      //A set of PolygonPoints/line segments that are actively being analyzed for intersections 
      TreeSet<PolygonPoint> active = new TreeSet<>(
         (o1, o2) -> {
            if (o1.y != o2.y) {
               return o1.y - o2.y;
            } else if (o1.x != o2.x) {
               return o1.x - o2.x;
            } else if (o1.shapeInd != o2.shapeInd) {
               return o1.shapeInd - o2.shapeInd;
            }
            return 0;
         });
         
      while(!stored.isEmpty()){
         PolygonPoint point;
         active.add(point = stored.poll());
         PolygonPoint p1 = point;
         while(p1.y == point.y){
            p1 = active.higher(p1);
            if(p1 == null){
               break;}
            if(compare(p1, point) || compare(p1.e1, point.e1) || compare(p1.e1, point) || compare(p1, point.e1)){
               return true;
            }
         }
         p1 = point;
         while(p1.y == point.y){
            p1 = active.lower(p1);
            if(p1 == null){
               break;}
            if(compare(p1, point) || compare(p1.e1, point.e1) || compare(p1.e1, point) || compare(p1, point.e1)){
               return true;
            }
         }
         
         //If the new point completes an active line segment then that segment is removed from active
         if(point.e1.x != point.x && active.contains(point.e1)) {
            active.remove(point.e1);
            PolygonPoint pL = point.e1, pU = point.e1;
            
            //checks for intersections between the lines that were previously seperated by point.e1
            do {
               pL = active.lower(pL);
            } while (pL != null && pL.y == point.y);
            do {
               pU = active.higher(pU);
            } while (pU != null && pU.y == point.y);
            if (pL != null && pU != null) {
               if (compare(pL, pU) || compare(pL.e1, pU.e1) || compare(pL.e1, pU) || compare(pL, pU.e1)) {
                  return true;
               }
            }
         }
         if(point.e2.x != point.x && active.contains(point.e2)) {
            active.remove(point.e2);
            PolygonPoint pL = point.e2, pU = point.e2;
            do {
               pL = active.lower(pL);
            } while (pL != null && pL.y == point.y);
            do {
               pU = active.higher(pU);
            } while (pU != null && pU.y == point.y);
            if (pL != null && pU != null) {
               if (compare(pL, pU) || compare(pL.e1, pU.e1) || compare(pL.e1, pU) || compare(pL, pU.e1)) {
                  return true;
               }
            }
         }
      }
      return false;
   }
   
   /**
    * Checks potential intersection of two line segments, uses method from <a href="https://www.geeksforgeeks.org/check-if-two-given-line-segments-intersect/">Geeks for Geeks</a>
    */
   private static boolean compare(PolygonPoint p, PolygonPoint p1){ 
      //returns false if one of the line segments is null or they are from the same shape
      if(p == null || p1 == null || p.shapeInd == p1.shapeInd){
         return false;
      }
      
      //Checks through the different rules for line intersection based on orientation of their points
      int one = getOrientation(p, p.e2, p1), two = getOrientation(p, p.e2, p1.e2), three = getOrientation(p1, p1.e2, p), four = getOrientation(p1, p1.e2, p.e2);
      return (one != two && three != four)
         || (one == 0 && two == 0 && three == 0 && four == 0 
            && ((Math.min(p.x,p.e2.x)<=Math.max(p1.x,p.e2.x) 
               && Math.min(p1.y,p1.e2.y)<=Math.max(p.y,p.e2.y)) 
            && ((Math.min(p.y,p.e2.y)<=Math.max(p1.y,p.e2.y) 
               && Math.min(p1.y,p1.e2.y)<=Math.max(p.y,p.e2.y)))));
   }
   
   /**
    * Returns the oriantation of the pair of line segments, as defined by <a href="https://www.geeksforgeeks.org/check-if-two-given-line-segments-intersect/">Geeks for Geeks</a>
    */
   private static int getOrientation(PolygonPoint p1, PolygonPoint p2, PolygonPoint p3){
      int test = (p2.y-p1.y)*(p3.x-p2.x)-(p3.y-p2.y)*(p2.x-p1.x);
      if(test < 0){
         return -1;}
      else if(test > 0){
         return 1;}
      else{
         return 0;}
   }
   
   /**
    * Converts the polygon into an array of PolygonPoints, which is then sorted into arr as line segments
    */
   private static void sortIn(PriorityQueue<PolygonPoint> arr, Polygon p, int num) {
      PolygonPoint[] points = new PolygonPoint[p.npoints];
      
      //Fills the array and properly instantiates the PolygonPoints and their endpoints
      points[0] = new PolygonPoint(p.xpoints[0], p.ypoints[0], num);
      for (int i = 1; i < points.length; i++) {
         points[i - 1].e2 = points[i] = new PolygonPoint(p.xpoints[i], p.ypoints[i], num);
         points[i].e1 = points[i - 1];
      }
      points[0].e1 = points[points.length - 1];
      points[points.length - 1].e2 = points[0];
      
      //Shifts all the points from the array to the PriorityQueue
      for(int i = 0; i < points.length; i++){
         arr.add(points[i]);
      }
   }
   
   /**
    * Util class that essentially acts as both a vertice on a closed polygon, and an endpoint of two lines
    */
   private static class PolygonPoint {
      //The position and shape index of the point
      public int x, y, shapeInd;
      
      //The endpoints of the line segments associated with this point
      public PolygonPoint e1, e2;
      
      public PolygonPoint(int x, int y, int num){
         this.x = x;
         this.y = y;
         shapeInd = num;
      }
   }
}