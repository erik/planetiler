package com.onthegomap.planetiler.geo;

import static com.onthegomap.planetiler.TestUtils.*;
import static com.onthegomap.planetiler.geo.GeoUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.geotools.geometry.jts.WKTReader2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.io.ParseException;

class GeoUtilsTest {

  @ParameterizedTest
  @CsvSource({
    "0,0, 0.5,0.5",
    "0, -180, 0, 0.5",
    "0, 180, 1, 0.5",
    "0, " + (180 - 1e-7) + ", 1, 0.5",
    "45, 0, 0.5, 0.359725",
    "-45, 0, 0.5, " + (1 - 0.359725),
    "86, -198, -0.05, -0.03391287",
    "-86, 198, 1.05, 1.03391287",
  })
  void testWorldCoords(double lat, double lon, double worldX, double worldY) {
    assertEquals(worldY, getWorldY(lat), 1e-5);
    assertEquals(worldX, getWorldX(lon), 1e-5);
    long encoded = encodeFlatLocation(lon, lat);
    assertEquals(worldY, decodeWorldY(encoded), 1e-5);
    assertEquals(worldX, decodeWorldX(encoded), 1e-5);

    Point input = newPoint(lon, lat);
    Point expected = newPoint(worldX, worldY);
    Geometry actual = latLonToWorldCoords(input);
    assertEquals(round(expected), round(actual));

    Geometry roundTripped = worldToLatLonCoords(actual);
    assertEquals(round(input), round(roundTripped));
  }

  @Test
  void testPolygonToLineString() throws GeometryException {
    assertEquals(newLineString(
      0, 0,
      1, 0,
      1, 1,
      0, 1,
      0, 0
    ), GeoUtils.polygonToLineString(rectangle(
      0, 1
    )));
  }

  @Test
  void testMultiPolygonToLineString() throws GeometryException {
    assertEquals(newLineString(
      0, 0,
      1, 0,
      1, 1,
      0, 1,
      0, 0
    ), GeoUtils.polygonToLineString(newMultiPolygon(rectangle(
      0, 1
    ))));
  }

  @Test
  void testLineRingToLineString() throws GeometryException {
    assertEquals(newLineString(
      0, 0,
      1, 0,
      1, 1,
      0, 1,
      0, 0
    ), GeoUtils.polygonToLineString(rectangle(
      0, 1
    ).getExteriorRing()));
  }

  @Test
  void testComplexPolygonToLineString() throws GeometryException {
    assertEquals(newMultiLineString(
      newLineString(
        0, 0,
        3, 0,
        3, 3,
        0, 3,
        0, 0
      ), newLineString(
        1, 1,
        2, 1,
        2, 2,
        1, 2,
        1, 1
      )
    ), GeoUtils.polygonToLineString(newPolygon(
      rectangleCoordList(
        0, 3
      ), List.of(rectangleCoordList(
        1, 2
      )))));
  }

  @ParameterizedTest
  @CsvSource({
    "0, 156543",
    "8, 611",
    "14, 9",
  })
  void testMetersPerPixel(int zoom, double meters) {
    assertEquals(meters, metersPerPixelAtEquator(zoom), 1);
  }

  @Test
  void testIsConvexTriangle() {
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      0, 1,
      0, 0
    ));
  }

  @Test
  void testIsConvexRectangle() {
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      0, 1,
      0, 0
    ));
  }

  @Test
  void testBarelyConvexRectangle() {
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      0.5, 0.5,
      0, 0
    ));
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      0.4, 0.4,
      0, 0
    ));
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      0.7, 0.7,
      0, 0
    ));
  }

  @Test
  void testConcaveRectangleDoublePoints() {
    assertConvex(true, newLinearRing(
      0, 0,
      0, 0,
      1, 0,
      1, 1,
      0, 1,
      0, 0
    ));
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      1, 0,
      1, 1,
      0, 1,
      0, 0
    ));
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      1, 1,
      0, 1,
      0, 0
    ));
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      0, 1,
      0, 1,
      0, 0
    ));
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      0, 1,
      0, 0,
      0, 0
    ));
  }

  @Test
  void testBarelyConcaveTriangle() {
    assertConvex(false, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      0.51, 0.5,
      0, 0
    ));
  }

  @Test
  void testAllowVerySmallConcavity() {
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      0.5001, 0.5,
      0, 0
    ));
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      0.5, 0.4999,
      0, 0
    ));
  }

  @Test
  void test5PointsConcave() {
    assertConvex(false, newLinearRing(
      0, 0,
      0.5, 0.1,
      1, 0,
      1, 1,
      0, 1,
      0, 0
    ));
    assertConvex(false, newLinearRing(
      0, 0,
      1, 0,
      0.9, 0.5,
      1, 1,
      0, 1,
      0, 0
    ));
    assertConvex(false, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      0.5, 0.9,
      0, 1,
      0, 0
    ));
    assertConvex(false, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      0, 1,
      0.1, 0.5,
      0, 0
    ));
  }

  @Test
  void test5PointsColinear() {
    assertConvex(true, newLinearRing(
      0, 0,
      0.5, 0,
      1, 0,
      1, 1,
      0, 1,
      0, 0
    ));
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      1, 0.5,
      1, 1,
      0, 1,
      0, 0
    ));
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      0.5, 1,
      0, 1,
      0, 0
    ));
    assertConvex(true, newLinearRing(
      0, 0,
      1, 0,
      1, 1,
      0, 1,
      0, 0.5,
      0, 0
    ));
  }

  private static void assertConvex(boolean isConvex, LinearRing ring) {
    for (double rotation : new double[]{0, 90, 180, 270}) {
      LinearRing rotated = (LinearRing) AffineTransformation.rotationInstance(Math.toRadians(rotation)).transform(ring);
      for (boolean flip : new boolean[]{false, true}) {
        LinearRing flipped = flip ? (LinearRing) AffineTransformation.scaleInstance(-1, 1).transform(rotated) : rotated;
        for (boolean reverse : new boolean[]{false, true}) {
          LinearRing reversed = reverse ? flipped.reverse() : flipped;
          for (double scale : new double[]{1, 1e-2, 1 / Math.pow(2, 14) / 4096}) {
            LinearRing scaled = (LinearRing) AffineTransformation.scaleInstance(scale, scale).transform(reversed);
            assertEquals(isConvex, isConvex(scaled),
              "rotation=" + rotation + " flip=" + flip + " reverse=" + reverse + " scale=" + scale);
          }
        }
      }
    }
  }

  @Test
  void testCombineEmpty() {
    assertEquals(EMPTY_GEOMETRY, GeoUtils.combine());
  }

  @Test
  void testCombineOne() {
    assertEquals(newLineString(0, 0, 1, 1), GeoUtils.combine(newLineString(0, 0, 1, 1)));
  }

  @Test
  void testCombineTwo() {
    assertEquals(GeoUtils.JTS_FACTORY.createGeometryCollection(new Geometry[]{
      newLineString(0, 0, 1, 1),
      newLineString(2, 2, 3, 3)
    }), GeoUtils.combine(
      newLineString(0, 0, 1, 1),
      newLineString(2, 2, 3, 3)
    ));
  }

  @Test
  void testCombineNested() {
    assertEquals(GeoUtils.JTS_FACTORY.createGeometryCollection(new Geometry[]{
      newLineString(0, 0, 1, 1),
      newLineString(2, 2, 3, 3),
      newLineString(4, 4, 5, 5)
    }), GeoUtils.combine(
      GeoUtils.combine(
        newLineString(0, 0, 1, 1),
        newLineString(2, 2, 3, 3)
      ),
      newLineString(4, 4, 5, 5)
    ));
  }

  @Test
  void testSnapAndFixIssue511() throws ParseException, GeometryException {
    var result = GeoUtils.snapAndFixPolygon(new WKTReader2().read(
      """
        MULTIPOLYGON (((198.83750000000003 46.07500000000004, 199.0625 46.375, 199.4375 46.0625, 199.5 46.43750000000001, 199.5625 46, 199.3125 45.5, 198.8912037037037 46.101851851851876, 198.83750000000003 46.07500000000004)), ((198.43750000000003 46.49999999999999, 198.5625 46.43750000000001, 198.6875 46.25, 198.1875 46.25, 198.43750000000003 46.49999999999999)), ((198.6875 46.25, 198.81249999999997 46.062500000000014, 198.6875 46.00000000000002, 198.6875 46.25)), ((196.55199579831933 46.29359243697479, 196.52255639097743 46.941259398496236, 196.5225563909774 46.941259398496236, 196.49999999999997 47.43750000000001, 196.875 47.125, 197 47.5625, 197.47880544905414 46.97729334004497, 197.51505401161464 46.998359569801956, 197.25 47.6875, 198.0625 47.6875, 198.5 46.625, 198.34375 46.546875, 198.34375000000003 46.54687499999999, 197.875 46.3125, 197.875 46.25, 197.875 46.0625, 197.82894736842107 46.20065789473683, 197.25 46.56250000000001, 197.3125 46.125, 196.9375 46.1875, 196.9375 46.21527777777778, 196.73250000000002 46.26083333333334, 196.5625 46.0625, 196.55199579831933 46.29359243697479)), ((196.35213414634146 45.8170731707317, 197.3402027027027 45.93108108108108, 197.875 45.99278846153846, 197.875 45.93750000000002, 197.93749999999997 45.99999999999999, 197.9375 46, 197.90625 45.96874999999999, 197.90625 45.96875, 196.75000000000006 44.81250000000007, 197.1875 45.4375, 196.3125 45.8125, 196.35213414634146 45.8170731707317)), ((195.875 46.124999999999986, 195.8125 46.5625, 196.5 46.31250000000001, 195.9375 46.4375, 195.875 46.124999999999986)), ((196.49999999999997 46.93749999999999, 196.125 46.875, 196.3125 47.125, 196.49999999999997 46.93749999999999)))
        """));
    assertEquals(3.146484375, result.getArea(), 1e-5);
  }

  @Test
  void testSnapAndFixIssue546() throws Exception {
    var result = GeoUtils.snapAndFixPolygon(new WKTReader2().read(
      """
         POLYGON ((253.4035000888889 182.15245755596698, 253.4035000888889 192.42460404536087, 231.72100551110998 192.42460404536087, 231.72100551110998 182.15245755596698, 253.4035000888889 182.15245755596698), (238.55449884444533 189.31978856920614, 239.24462933333416 188.80766307766316, 239.7510769777782 188.9074400583977, 240.215836444444 188.75998568623527, 240.93891697777872 188.97921961517477, 241.14044017777815 188.58300109059564, 241.4815459555557 188.55851237651768, 241.79420728888908 188.21465913393877, 242.22633528888946 188.38327155030674, 243.18884977777816 188.05397327635546, 245.1478755555563 186.52293889107023, 246.10438257777787 186.36621538112558, 248.2394453333327 186.42034042916566, 249.1234986666659 186.19223044982573, 249.40157155555607 185.91447652198895, 248.8409656888889 185.86637488552878, 248.65609955555556 185.70381662276668, 249.15735893333294 185.6638057320315, 248.9912433777772 185.4383152924911, 249.2732757333324 185.39982839753793, 249.13583217777705 185.26817533179565, 249.64082346666692 185.35431552834962, 249.58625564444446 185.2387741210805, 250.07663786666672 184.98889126774066, 249.89322808888755 184.80971525737732, 250.16206222222172 184.99232805109295, 250.8118698666658 184.6281615338703, 250.75548159999926 184.24722470205597, 250.920868977777 184.51748286988732, 251.60071395555497 184.44186173742492, 252.2763719111117 184.14592938361147, 252.47643875555514 184.38816202545058, 252.52927715555597 184.06594510054947, 252.05427768888876 183.63535592396693, 251.38084977777726 183.45587597044323, 249.45217991110985 183.57736412501072, 237.92808391111066 186.77003215242803, 236.26023822222123 187.38283697317274, 235.82260337777734 187.77645481522813, 235.52000000000044 188.16559966981094, 235.5810304000006 188.4274450309258, 234.87101155555501 189.3390088660126, 234.41658311111132 190.54630378141792, 235.19054506666726 190.90294646300754, 235.06438826666636 190.9218524646094, 235.42788551111153 190.82579663907563, 234.94578631111108 190.56178096487292, 236.14332017777815 190.77105879575356, 237.18561564444462 190.22218090320712, 236.9983374222229 190.1437186356561, 237.4851242666664 190.2181396470678, 237.212831288889 190.0961370959203, 237.46123093333335 189.99685413991574, 237.61637831110966 189.3895760086525, 237.84302364444375 189.34350883971456, 237.6822328888884 189.49124180390208, 238.38319502222112 189.22452438824712, 238.55449884444533 189.31978856920614))
        """));

    // Fails on IsInvalidOp.checkShellsNotNested() (i.e. IndexedNestedPolygonTester)
    assertTrue(result.isValid());
  }
}
