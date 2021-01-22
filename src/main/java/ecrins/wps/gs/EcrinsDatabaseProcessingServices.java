package ecrins.wps.gs;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.process.StreamRawData;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.locationtech.jts.geom.Point;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@DescribeProcess(
    title = "Ecrins database processing services",
    description = "Various processing to compute upstream or downstream analysis on Ecrins database."
)
public class EcrinsDatabaseProcessingServices implements GeoServerProcess {

    private Catalog catalog;
    private GeoServer geoserver;

    Map<String, String> processLayers = new HashMap<>();
    Map<String, String> mimeTypes = new HashMap<>();
    Map<String, String> extensions = new HashMap<>();

    public enum QueryType {
        nearestriver,
        upstreamriver,
        downstreamriver,
        upstreamriverbasin
    }

    public enum OutputType {
        text("text/plain"),
        shp("shape-zip"),
        json("json"),
        gml2("gml2"),
        gml3("gml3"),
        csv("csv");

        public String type;

        OutputType(String type) {
            this.type = type;
        }
    }
    public EcrinsDatabaseProcessingServices(Catalog catalog, GeoServer geoserver) {
        this.catalog = catalog;
        this.geoserver = geoserver;

        processLayers.put("nearestriver", "ecrins%3Anearestriver");
        processLayers.put("upstreamriver", "ecrins%3Aupstreamriver");
        processLayers.put("downstreamriver", "ecrins%3Adownstreamriver");
        processLayers.put("upstreamriverbasin", "ecrins%3Aupstreamriverbasin");

        mimeTypes.put("json", "application/json");
        mimeTypes.put("csv", "text/csv");
        mimeTypes.put("gml2", "application/xml");
        mimeTypes.put("gml3", "application/xml");
        mimeTypes.put("shape-zip", "application/zip");

        extensions.put("json", "json");
        extensions.put("csv", "csv");
        extensions.put("gml2", "xml");
        extensions.put("gml3", "xml");
        extensions.put("shape-zip", "zip");
    }


    @DescribeResult(
        name = "result",
        description = "Ecrins database processing services",
        meta = {
            "mimeTypes=json,text/plain,shape-zip,gml2,gml3,csv",
            "chosenMimeType=outputMimeType"
        }
    )
    public StreamRawData execute(
        @DescribeParameter(
            name = "Analysis",
            description = "Query (one of nearestriver, upstreamriver, downstreamriver, upstreamriverbasin)",
            defaultValue = "nearestriver",
            min = 1,
            max = 1,
            meta = { "mimeTypes=text/plain" }
        ) final QueryType query,
        @DescribeParameter(
            name = "Point",
            description = "Starting point of the analysis eg. a WKT like POINT(18.4567853 53.4047983)",
            min = 1,
            max = 1
            // defaultValue = "DEFAULT_LOCATION"
        ) final Point location,
        @DescribeParameter(
            name = "CRS",
            description = "Coordinate reference system of the point and the generated geometry",
            defaultValue = "3857",
            min = 1,
            max = 1
        ) final Integer crs,

        // This parameter will contain the output mime type the user has chosen.
        // It will NOT appear as a usual parameter in the DescribeProcess of the
        // WPS!
        @DescribeParameter(
            name = "outputMimeType",
            description = "Output type (one of text/plain,shape-zip,json,gml2,gml3,csv)",
            min = 0,
            max = 1
        ) final OutputType outputMimeType
    ) throws IOException {

        System.out.println(location);

        String geoserverUrl = geoserver.getGlobal().getSettings().getProxyBaseUrl() != null
            ? geoserver.getGlobal().getSettings().getProxyBaseUrl()
            : "http://localhost:8080/geoserver";
        String url = String.format("%s/ecrins/ows?" +
                "service=WFS&version=2.0.0&request=GetFeature&srsName=EPSG:%s&" +
                "typeName=%s&viewparams=lon:%s;lat:%s;srs:%s&outputFormat=%s",
                geoserverUrl, crs,
                processLayers.get(query.name()),
                location.getX(), location.getY(),
                crs, outputMimeType.type);

        System.out.println(url);
//        return new StringRawData(
//            mimeTypes.get(outputMimeType.type),
//            IOUtils.toString(new URL(url).openStream()));
        return new StreamRawData(
            mimeTypes.get(outputMimeType),
            new URL(url).openStream(),
            extensions.get(outputMimeType));
    }
}
