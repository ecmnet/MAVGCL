package com.comino.jfx.extensions;


import eu.hansolo.medusa.Clock;
import eu.hansolo.medusa.Clock.ClockSkinType;
import eu.hansolo.medusa.ClockBuilder;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.Section;
import eu.hansolo.tilesfx.Country;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.TimeSection;
import eu.hansolo.tilesfx.TimeSectionBuilder;
import eu.hansolo.tilesfx.skins.BarChartItem;
import eu.hansolo.tilesfx.skins.LeaderBoardItem;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import javafx.scene.Scene;


import java.time.LocalTime;
import java.util.Locale;
import java.util.Random;




/**
 * User: hansolo
 * Date: 08.01.17
 * Time: 07:33
 */
public class Tiles_demo extends Application {
    private static final double TILE_SIZE = 200;
    private static final Random RND       = new Random();

    private BarChartItem      barChartItem1;
    private BarChartItem      barChartItem2;
    private BarChartItem      barChartItem3;
    private BarChartItem      barChartItem4;
    private LeaderBoardItem   leaderBoardItem1;
    private LeaderBoardItem   leaderBoardItem2;
    private LeaderBoardItem   leaderBoardItem3;
    private LeaderBoardItem   leaderBoardItem4;
    private Tile              percentageTile;
    private Tile              clockTile;
    private Tile              gaugeTile;
    private Tile              sparkLineTile;
    private Tile              areaChartTile;
    private Tile              lineChartTile;
    private Tile              highLowTile;
    private Tile              timerControlTile;
    private Tile              numberTile;
    private Tile              textTile;
    private Tile              plusMinusTile;
    private Tile              sliderTile;
    private Tile              switchTile;
    private Tile              worldTile;
    private Tile              timeTile;
    private Tile              barChartTile;
    private Tile              customTile;
    private Tile              leaderBoardTile;
    private Gauge             indicatorGauge;
    private Tile              indicatorTile;
    private Gauge             slimGauge;
    private Tile              slimTile;
    private Gauge             dashboardGauge;
    private Tile              dashboardTile;
    private Gauge             digitalGauge;
    private Tile              digitalTile;
    private Gauge             simpleDigitalGauge;
    private Tile              simpleDigitalTile;
    private Gauge             simpleSectionGauge;
    private Tile              simpleSectionTile;
    private Gauge             bulletChartGauge;
    private Tile              bulletChartTile;
    private Gauge             spaceXGauge;
    private Tile              spaceXTile;
    private Clock             slimClock;
    private Tile              slimClockTile;


    private long              lastTimerCall;
    private AnimationTimer    timer;


    @Override public void init() {
        // LineChart Data
        XYChart.Series<String, Number> series1 = new XYChart.Series();
        series1.setName("Whatever");
        series1.getData().add(new XYChart.Data("MO", 23));
        series1.getData().add(new XYChart.Data("TU", 21));
        series1.getData().add(new XYChart.Data("WE", 20));
        series1.getData().add(new XYChart.Data("TH", 22));
        series1.getData().add(new XYChart.Data("FR", 24));
        series1.getData().add(new XYChart.Data("SA", 22));
        series1.getData().add(new XYChart.Data("SU", 20));

        XYChart.Series<String, Number> series2 = new XYChart.Series();
        series2.setName("Inside");
        series2.getData().add(new XYChart.Data("MO", 8));
        series2.getData().add(new XYChart.Data("TU", 5));
        series2.getData().add(new XYChart.Data("WE", 0));
        series2.getData().add(new XYChart.Data("TH", 2));
        series2.getData().add(new XYChart.Data("FR", 4));
        series2.getData().add(new XYChart.Data("SA", 3));
        series2.getData().add(new XYChart.Data("SU", 5));

        XYChart.Series<String, Number> series3 = new XYChart.Series();
        series3.setName("Outside");
        series3.getData().add(new XYChart.Data("MO", 8));
        series3.getData().add(new XYChart.Data("TU", 5));
        series3.getData().add(new XYChart.Data("WE", 0));
        series3.getData().add(new XYChart.Data("TH", 2));
        series3.getData().add(new XYChart.Data("FR", 4));
        series3.getData().add(new XYChart.Data("SA", 3));
        series3.getData().add(new XYChart.Data("SU", 5));

        // WorldMap Data
        for (int i = 0; i < Country.values().length ; i++) {
            double value = RND.nextInt(10);
            Color  color;
            if (value > 8) {
                color = Tile.RED;
            } else if (value > 6) {
                color = Tile.ORANGE;
            } else if (value > 4) {
                color = Tile.YELLOW_ORANGE;
            } else if (value > 2) {
                color = Tile.GREEN;
            } else {
                color = Tile.BLUE;
            }
            Country.values()[i].setColor(color);
        }

        // TimeControl Data
        TimeSection timeSection = TimeSectionBuilder.create()
                                                    .start(LocalTime.now().plusSeconds(20))
                                                    .stop(LocalTime.now().plusHours(1))
                                                    //.days(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
                                                    .color(Tile.GRAY)
                                                    .highlightColor(Tile.RED)
                                                    .build();

        timeSection.setOnTimeSectionEntered(e -> System.out.println("Section ACTIVE"));
        timeSection.setOnTimeSectionLeft(e -> System.out.println("Section INACTIVE"));

        // BarChart Items
        barChartItem1 = new BarChartItem("Gerrit", 47, Tile.BLUE);
        barChartItem2 = new BarChartItem("Sandra", 43, Tile.RED);
        barChartItem3 = new BarChartItem("Lilli", 12, Tile.GREEN);
        barChartItem4 = new BarChartItem("Anton", 8, Tile.ORANGE);

        // LeaderBoard Items
        leaderBoardItem1 = new LeaderBoardItem("Gerrit", 47);
        leaderBoardItem2 = new LeaderBoardItem("Sandra", 43);
        leaderBoardItem3 = new LeaderBoardItem("Lilli", 12);
        leaderBoardItem4 = new LeaderBoardItem("Anton", 8);

        // Creating Tiles
        percentageTile = TileBuilder.create()
                                    .prefSize(TILE_SIZE, TILE_SIZE)
                                    .skinType(SkinType.PERCENTAGE)
                                    .title("Percentage Tile")
                                    .unit("\u0025")
                                    .description("Test")
                                    .maxValue(60)
                                    .build();

        clockTile = TileBuilder.create()
                               .prefSize(TILE_SIZE, TILE_SIZE)
                               .skinType(SkinType.CLOCK)
                               .title("Clock Tile")
                               .text("Whatever text")
                               .dateVisible(true)
                               .locale(Locale.US)
                               .running(true)
                               .build();

        gaugeTile = TileBuilder.create()
                               .prefSize(TILE_SIZE, TILE_SIZE)
                               .skinType(SkinType.GAUGE)
                               .title("Gauge Tile")
                               .unit("V")
                               .threshold(75)
                               .build();

        sparkLineTile = TileBuilder.create()
                                   .prefSize(TILE_SIZE, TILE_SIZE)
                                   .skinType(SkinType.SPARK_LINE)
                                   .title("SparkLine Tile")
                                   .unit("mb")
                                   .gradientStops(new Stop(0, Tile.GREEN),
                                                  new Stop(0.5, Tile.YELLOW),
                                                  new Stop(1.0, Tile.RED))
                                   .strokeWithGradient(true)
                                   .build();

        areaChartTile = TileBuilder.create()
                                   .prefSize(TILE_SIZE, TILE_SIZE)
                                   .skinType(SkinType.AREA_CHART)
                                   .title("AreaChart Tile")
                                   .series(series1)
                                   .build();

        lineChartTile = TileBuilder.create()
                                   .prefSize(TILE_SIZE, TILE_SIZE)
                                   .skinType(SkinType.LINE_CHART)
                                   .title("LineChart Tile")
                                   .series(series2, series3)
                                   .build();

        highLowTile = TileBuilder.create()
                                 .prefSize(TILE_SIZE, TILE_SIZE)
                                 .skinType(SkinType.HIGH_LOW)
                                 .title("HighLow Tile")
                                 .unit("\u0025")
                                 .description("Test")
                                 .text("Whatever text")
                                 .referenceValue(6.7)
                                 .value(8.2)
                                 .build();

        timerControlTile = TileBuilder.create()
                                      .prefSize(TILE_SIZE, TILE_SIZE)
                                      .skinType(SkinType.TIMER_CONTROL)
                                      .title("TimerControl Tile")
                                      .text("Whatever text")
                                      .secondsVisible(true)
                                      .dateVisible(true)
                                      .timeSections(timeSection)
                                      .running(true)
                                      .build();

        numberTile = TileBuilder.create()
                                .prefSize(TILE_SIZE, TILE_SIZE)
                                .skinType(SkinType.NUMBER)
                                .title("Number Tile")
                                .text("Whatever text")
                                .value(13)
                                .unit("mb")
                                .description("Test")
                                .textVisible(true)
                                .build();

        textTile = TileBuilder.create()
                              .prefSize(TILE_SIZE, TILE_SIZE)
                              .skinType(SkinType.TEXT)
                              .title("Text Tile")
                              .text("Whatever text")
                              .description("May the force be with you\n...always")
                              .textVisible(true)
                              .build();

        plusMinusTile = TileBuilder.create()
                                   .prefSize(TILE_SIZE, TILE_SIZE)
                                   .skinType(SkinType.PLUS_MINUS)
                                   .maxValue(30)
                                   .minValue(0)
                                   .title("PlusMinus Tile")
                                   .text("Whatever text")
                                   .description("Test")
                                   .unit("\u00B0C")
                                   .build();

        sliderTile = TileBuilder.create()
                                .prefSize(TILE_SIZE, TILE_SIZE)
                                .skinType(SkinType.SLIDER)
                                .title("Slider Tile")
                                .text("Whatever text")
                                .description("Test")
                                .unit("\u00B0C")
                                .barBackgroundColor(Tile.FOREGROUND)
                                .build();

        switchTile = TileBuilder.create()
                                .prefSize(TILE_SIZE, TILE_SIZE)
                                .skinType(SkinType.SWITCH)
                                .title("Switch Tile")
                                .text("Whatever text")
                                //.description("Test")
                                .build();

        switchTile.setOnSwitchPressed(e -> System.out.println("Switch pressed"));
        switchTile.setOnSwitchReleased(e -> System.out.println("Switch released"));

        worldTile = TileBuilder.create()
                               .minSize(2 * TILE_SIZE + 10, TILE_SIZE)
                               .maxSize(2 * TILE_SIZE + 10, TILE_SIZE)
                               .prefSize(2 * TILE_SIZE + 10, TILE_SIZE)
                               .skinType(SkinType.WORLDMAP)
                               .title("WorldMap Tile")
                               .text("Whatever text")
                               .textVisible(false)
                               .build();

        timeTile = TileBuilder.create()
                              .prefSize(TILE_SIZE, TILE_SIZE)
                              .skinType(SkinType.TIME)
                              .title("Time Tile")
                              .text("Whatever text")
                              .duration(LocalTime.of(1, 22))
                              .description("Average reply time")
                              .textVisible(true)
                              .build();

        barChartTile = TileBuilder.create()
                                  .prefSize(TILE_SIZE, TILE_SIZE)
                                  .skinType(SkinType.BAR_CHART)
                                  .title("BarChart Tile")
                                  .text("Whatever text")
                                  .barChartItems(barChartItem1, barChartItem2, barChartItem3, barChartItem4)
                                  .decimals(0)
                                  .build();

        customTile = TileBuilder.create()
                                .prefSize(TILE_SIZE, TILE_SIZE)
                                .skinType(SkinType.CUSTOM)
                                .title("Custom Tile")
                                .text("Whatever text")
                                .graphic(new Button("Click Me"))
                                .roundedCorners(false)
                                .build();

        leaderBoardTile = TileBuilder.create()
                                     .prefSize(TILE_SIZE, TILE_SIZE)
                                     .skinType(SkinType.LEADER_BOARD)
                                     .title("LeaderBoard Tile")
                                     .text("Whatever text")
                                     .leaderBoardItems(leaderBoardItem1, leaderBoardItem2, leaderBoardItem3, leaderBoardItem4)
                                     .build();



        slimGauge = createGauge(Gauge.SkinType.SLIM);
        slimTile  = TileBuilder.create()
                               .prefSize(TILE_SIZE, TILE_SIZE)
                               .skinType(SkinType.CUSTOM)
                               .title("Medusa Slim")
                               .text("Temperature")
                               .graphic(slimGauge)
                               .build();

        dashboardGauge = createGauge(Gauge.SkinType.DASHBOARD);
        dashboardTile  = TileBuilder.create()
                                    .prefSize(TILE_SIZE, TILE_SIZE)
                                    .skinType(SkinType.CUSTOM)
                                    .title("Medusa Dashboard")
                                    .text("Temperature")
                                    .graphic(dashboardGauge)
                                    .build();

        digitalGauge = createGauge(Gauge.SkinType.DIGITAL);
        digitalTile  = TileBuilder.create()
                                  .prefSize(TILE_SIZE, TILE_SIZE)
                                  .skinType(SkinType.CUSTOM)
                                  .title("Medusa Digital")
                                  .text("Temperature")
                                  .graphic(digitalGauge)
                                  .build();

        simpleDigitalGauge = createGauge(Gauge.SkinType.SIMPLE_DIGITAL);
        simpleDigitalTile  = TileBuilder.create()
                                        .prefSize(TILE_SIZE, TILE_SIZE)
                                        .skinType(SkinType.CUSTOM)
                                        .title("Medusa SimpleDigital")
                                        .text("Temperature")
                                        .graphic(simpleDigitalGauge)
                                        .build();

        indicatorGauge = createGauge(Gauge.SkinType.INDICATOR);
        indicatorTile  = TileBuilder.create()
                                    .prefSize(TILE_SIZE, TILE_SIZE)
                                    .skinType(SkinType.CUSTOM)
                                    .title("Medusa Indicator")
                                    .text("")
                                    .graphic(indicatorGauge)
                                    .build();
        indicatorTile.textProperty().bind(indicatorGauge.currentValueProperty().asString("%.0f"));

        simpleSectionGauge = createGauge(Gauge.SkinType.SIMPLE_SECTION);
        simpleSectionGauge.setBarColor(Tile.FOREGROUND);
        simpleSectionGauge.setSections(new Section(66, 100, Tile.BLUE));
        simpleSectionTile  = TileBuilder.create()
                                        .prefSize(TILE_SIZE, TILE_SIZE)
                                        .skinType(SkinType.CUSTOM)
                                        .title("Medusa SimpleSection")
                                        .text("Temperature")
                                        .graphic(simpleSectionGauge)
                                        .build();

        bulletChartGauge = createGauge(Gauge.SkinType.BULLET_CHART);
        bulletChartGauge.setThreshold(75);
        bulletChartGauge.setBarColor(Tile.FOREGROUND);
        bulletChartGauge.setThresholdColor(Tile.RED);
        bulletChartGauge.setSections(new Section(0, 33, Tile.BLUE.darker().darker()),
                                     new Section(33, 66, Tile.BLUE.darker()),
                                     new Section(66, 100, Tile.BLUE));
        bulletChartTile = TileBuilder.create()
                                     .prefSize(2 * TILE_SIZE + 10, TILE_SIZE)
                                     .skinType(SkinType.CUSTOM)
                                     .title("Medusa BulletChart")
                                     .text("Temperature")
                                     .graphic(bulletChartGauge)
                                     .build();

        slimClock = ClockBuilder.create()
                                .prefSize(TILE_SIZE, TILE_SIZE)
                                .skinType(ClockSkinType.SLIM)
                                .secondColor(Tile.FOREGROUND)
                                .minuteColor(Tile.BLUE)
                                .hourColor(Tile.FOREGROUND)
                                .dateColor(Tile.FOREGROUND)
                                .running(true)
                                .build();
        slimClockTile = TileBuilder.create()
                                   .prefSize(TILE_SIZE, TILE_SIZE)
                                   .skinType(SkinType.CUSTOM)
                                   .title("Medusa Clock")
                                   .graphic(slimClock)
                                   .textVisible(false)
                                   .build();

        spaceXGauge = createGauge(Gauge.SkinType.SPACE_X);
        spaceXGauge.setThresholdColor(Tile.BLUE);
        spaceXGauge.setThreshold(85);
        spaceXGauge.setBarColor(Tile.FOREGROUND);
        spaceXTile = TileBuilder.create()
                                .prefSize(TILE_SIZE, TILE_SIZE)
                                .skinType(SkinType.CUSTOM)
                                .title("Medusa SpaceX")
                                .text("Temperature")
                                .graphic(spaceXGauge)
                                .build();




        lastTimerCall = System.nanoTime();
        timer = new AnimationTimer() {
            @Override public void handle(final long now) {
                if (now > lastTimerCall + 2_000_000_000) {
                    percentageTile.setValue(RND.nextDouble() * percentageTile.getRange() * 1.5 + percentageTile.getMinValue());
                    gaugeTile.setValue(RND.nextDouble() * gaugeTile.getRange() * 1.5 + gaugeTile.getMinValue());

                    sparkLineTile.setValue(RND.nextDouble() * sparkLineTile.getRange() + sparkLineTile.getMinValue());

                    highLowTile.setValue(RND.nextDouble() * 10);
                    series1.getData().forEach(data -> data.setYValue(RND.nextInt(100)));
                    series2.getData().forEach(data -> data.setYValue(RND.nextInt(30)));
                    series3.getData().forEach(data -> data.setYValue(RND.nextInt(10)));

                    barChartTile.getBarChartItems().get(RND.nextInt(3)).setValue(RND.nextDouble() * 80);

                    leaderBoardTile.getLeaderBoardItems().get(RND.nextInt(3)).setValue(RND.nextDouble() * 80);

                    slimGauge.setValue(RND.nextDouble() * 100);
                    dashboardGauge.setValue(RND.nextDouble() * 100);
                    digitalGauge.setValue(RND.nextDouble() * 100);
                    simpleDigitalGauge.setValue(RND.nextDouble() * 100);
                    indicatorGauge.setValue(RND.nextDouble() * 100);
                    simpleSectionGauge.setValue(RND.nextDouble() * 100);
                    bulletChartGauge.setValue(RND.nextDouble() * 100);
                    spaceXGauge.setValue(RND.nextDouble() * 100);



                    lastTimerCall = now;
                }
            }
        };
    }

    @Override public void start(Stage stage) {
        FlowPane pane = new FlowPane(percentageTile, clockTile, gaugeTile, sparkLineTile, areaChartTile,
                                     lineChartTile, timerControlTile, numberTile, textTile,
                                     highLowTile, plusMinusTile, sliderTile, switchTile, worldTile, timeTile,
                                     barChartTile, customTile, leaderBoardTile,
                                     slimTile, dashboardTile, digitalTile,
                                     simpleDigitalTile, indicatorTile, simpleSectionTile,
                                     bulletChartTile, slimClockTile, spaceXTile
                                     );
        pane.setPadding(new Insets(10));
        pane.setHgap(10);
        pane.setVgap(10);
        pane.setCenterShape(true);
        pane.setPrefSize(1270,  1060);
        pane.setBackground(new Background(new BackgroundFill(Tile.BACKGROUND.darker(), CornerRadii.EMPTY, Insets.EMPTY)));

        Scene scene = new Scene(pane);

        stage.setTitle("TilesFX Dashboard using Medusa");
        stage.setScene(scene);
        stage.show();

        timer.start();
    }

    private Gauge createGauge(final Gauge.SkinType TYPE) {
        return GaugeBuilder.create()
                           .skinType(TYPE)
                           .prefSize(TILE_SIZE, TILE_SIZE)
                           .animated(true)
                           //.title("")
                           .unit("\u00B0C")
                           .valueColor(Tile.FOREGROUND)
                           .titleColor(Tile.FOREGROUND)
                           .unitColor(Tile.FOREGROUND)
                           .barColor(Tile.BLUE)
                           .needleColor(Tile.FOREGROUND)
                           .barColor(Tile.BLUE)
                           .barBackgroundColor(Tile.BACKGROUND.darker())
                           .tickLabelColor(Tile.FOREGROUND)
                           .majorTickMarkColor(Tile.FOREGROUND)
                           .minorTickMarkColor(Tile.FOREGROUND)
                           .mediumTickMarkColor(Tile.FOREGROUND)
                           .build();
    }

    @Override public void stop() {
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
