/*
 * PrismsWmsRequest.java Created Mar 9, 2009 by Andrew Butler, PSL
 */
package prisms.arch.wms;

import java.awt.Color;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/** Represents a WMS request. This class makes it easy to modularize WMS implementations. */
public class PrismsWmsRequest
{
	private static final Color BG = new Color(.5f, .5f, .5f, .5f);

	/** The maximum allowable width for WMS overlays */
	public static final int MAX_WIDTH = 2048;

	/** The maximum allowable height for WMS overlays */
	public static final int MAX_HEIGHT = 2048;

	/** The SRS string corresponding to a plain lat/lon projection */
	public static final String EPSG_LATLON = "EPSG:4326";

	/** The SRS string corresponding to google's mercator projection */
	public static final String EPSG_GOOGLE = "EPSG:900913";

	/**
	 * The type for a WMS request. The request parameter must fit one of these types or it is not a
	 * WMS request.
	 */
	public static enum RequestType
	{
		/**
		 * Gets the capabilities of the server
		 */
		GetCapabilities("Capabilities", "GetCapabilities"),
		/**
		 * ???
		 */
		GetList("GetList"),
		/**
		 * Gets a map overlay
		 */
		Map("map", "GetMap"),
		/**
		 * Gets feature info for a point from an overlay
		 */
		GetFeatureInfo("feature_info", "GetFeatureInfo"),
		/**
		 * For any other type of WMS functionality
		 */
		Other("Other");

		private final String [] theNames;

		private RequestType(String... names)
		{
			theNames = names;
		}

		/**
		 * @param name The value of the request parameter in the WMS request
		 * @return The RequestType matching the given parameter value
		 */
		public static RequestType byName(String name)
		{
			for(RequestType type : values())
				for(String tName : type.theNames)
					if(tName.equalsIgnoreCase(name))
						return type;
			return null;
		}
	}

	private static java.util.regex.Pattern VERSION_PATTERN;

	static
	{
		VERSION_PATTERN = java.util.regex.Pattern.compile("[[\\d+].]*[\\d+]");
	}

	private String theRemoteAddress;

	private String theServletPath;

	private String theScheme;

	private String theServerName;

	private String theContextPath;

	private int thePort;

	private int [] theVersion;

	private RequestType theRequest;

	private String theFormat;

	private String theService;

	private java.util.Map<String, String> theRequestParams;

	private int theWidth;

	private int theHeight;

	private int theSelectedX;

	private int theSelectedY;

	private String [] theLayers;

	private BoundingBox theBounds;

	private String theSRS;

	private boolean isTransparent;

	private Color theBackgroundColor;

	/**
	 * Inspects an HTTP request to see if it is intended to be a WMS request
	 * 
	 * @param request The HTTP request to inspect
	 * @return Wheither the given request was intended to be a WMS request
	 */
	public static boolean isWMS(javax.servlet.http.HttpServletRequest request)
	{
		Map<String, String []> paramMap = request.getParameterMap();
		String version = getParameter(paramMap, "version");
		if(version == null)
			version = getParameter(paramMap, "wmtver"); // Version 1.0
		if(version == null)
			return false;
		java.util.regex.Matcher vMatcher = VERSION_PATTERN.matcher(version);
		if(!vMatcher.matches())
			return false;
		String requestType = getParameter(paramMap, "request");
		if(requestType == null)
			return false;
		return true;
	}

	/**
	 * Parses a WMS request from an HTTP request
	 * 
	 * @param request The HTTP request to parse the WMS request from
	 * @return The WMS request represented by the given HTTP request
	 */
	public static PrismsWmsRequest parseWMS(HttpServletRequest request)
	{
		PrismsWmsRequest ret = parseWMS(request.getParameterMap());
		ret.theServerName = request.getServerName();
		ret.thePort = request.getServerPort();
		ret.theContextPath = request.getContextPath();
		ret.theRemoteAddress = request.getRemoteAddr();
		ret.theScheme = request.getScheme();
		ret.theServletPath = request.getServletPath();
		return ret;
	}

	/**
	 * Parses a WMS request from a set of parameters
	 * 
	 * @param paramMap The parameters from the HTTP request
	 * @return The WMS request represented by the HTTP request with the given parameters
	 */
	public static PrismsWmsRequest parseWMS(Map<String, String []> paramMap)
	{
		String version = getParameter(paramMap, "version");
		if(version == null)
			version = getParameter(paramMap, "wmtver"); // Version 1.0
		if(version == null)
			throw new IllegalArgumentException("No WMS version specified");
		java.util.regex.Matcher vMatcher = VERSION_PATTERN.matcher(version);
		if(!vMatcher.matches())
			throw new IllegalArgumentException("WMS version supplied is malformatted");
		String requestType = getParameter(paramMap, "request");
		if(requestType == null)
			throw new IllegalArgumentException("No request parameter specified");
		RequestType rType = RequestType.byName(requestType);
		if(rType == null)
			throw new IllegalArgumentException("Unrecognized request type: " + requestType);

		PrismsWmsRequest ret = new PrismsWmsRequest();
		ret.theRequestParams = new java.util.HashMap<String, String>();
		for(java.util.Map.Entry<String, String []> entry : paramMap.entrySet())
		{
			String pName = entry.getKey();
			if(pName.equalsIgnoreCase("service"))
				ret.theService = entry.getValue()[0];
			else if(pName.equalsIgnoreCase("format"))
				ret.theFormat = entry.getValue()[0];
			else if(pName.equalsIgnoreCase("version") || pName.equalsIgnoreCase("wtmver")
				|| pName.equalsIgnoreCase("request") || ret.theRequestParams.containsKey(pName))
			{}// Do nothing
			else
				ret.theRequestParams.put(pName, entry.getValue()[0]);
		}
		String [] vSplit = version.split("\\.");
		ret.theVersion = new int [vSplit.length];
		for(int v = 0; v < ret.theVersion.length; v++)
			ret.theVersion[v] = Integer.parseInt(vSplit[v]);
		ret.theRequest = rType;
		switch(rType)
		{
		case GetCapabilities:
			fillCapabilities(ret);
			break;
		case GetFeatureInfo:
			fillFeatureInfo(ret);
			break;
		case Map:
			fillMap(ret);
			break;
		case GetList:
			fillList(ret);
			break;
		case Other:
			break;
		}
		return ret;
	}

	private PrismsWmsRequest()
	{
	}

	/**
	 * Creates a WMS request from code
	 * 
	 * @param srs The SRS string corresponding to the projection represented in this request
	 * @param minLat The minimum latitude of the viewing window
	 * @param minLon The minimum longitude of the viewing window
	 * @param maxLat The maximum latitude of the viewing window
	 * @param maxLon The maximum longitude of the viewing window
	 * @param width The width of the viewing window
	 * @param height The height of the viewing window
	 */
	public PrismsWmsRequest(String srs, float minLat, float minLon, float maxLat, float maxLon,
		int width, int height)
	{
		this();
		theSRS = srs;
		theBounds = new BoundingBox(minLat, maxLat, minLon, maxLon);
		theWidth = width;
		theHeight = height;
	}

	/**
	 * @return The address from which this request was made
	 * @see HttpServletRequest#getRemoteAddr()
	 */
	public String getRemoteAddress()
	{
		return theRemoteAddress;
	}

	/** @param ra The address from which this request was made */
	public void setRemoteAddress(String ra)
	{
		theRemoteAddress = ra;
	}

	/**
	 * @return The servlet path at which this request was made
	 * @see HttpServletRequest#getServletPath()
	 */
	public String getServletPath()
	{
		return theServletPath;
	}

	/** @param sp The servlet path at which this request was made */
	public void setServletPath(String sp)
	{
		theServletPath = sp;
	}

	/**
	 * @return The scheme of the request
	 * @see HttpServletRequest#getScheme()
	 */
	public String getScheme()
	{
		return theScheme;
	}

	/** @param s The schem of the request */
	public void setScheme(String s)
	{
		theScheme = s;
	}

	/**
	 * @return The name of the server that was invoked for this request
	 * @see HttpServletRequest#getServerName()
	 */
	public String getServerName()
	{
		return theServerName;
	}

	/** @param sn The name of the server that was invoked for this request */
	public void setServerName(String sn)
	{
		theServerName = sn;
	}

	/**
	 * @return The context path of the request
	 * @see HttpServletRequest#getContextPath()
	 */
	public String getContextPath()
	{
		return theContextPath;
	}

	/** @param cp The context path of the request */
	public void setContextPath(String cp)
	{
		theContextPath = cp;
	}

	/**
	 * @return The port at which this request was invoked
	 * @see HttpServletRequest#getServerPort()
	 */
	public int getPort()
	{
		return thePort;
	}

	/** @param port The port at which this request was invoked */
	public void setPort(int port)
	{
		thePort = port;
	}

	/**
	 * @param name The name of the parameter to get--not case sensitive
	 * @return The value of the given parameter, or null if it was unspecified
	 */
	public String getParameter(String name)
	{
		for(Map.Entry<String, String> entry : theRequestParams.entrySet())
			if(entry.getKey().equalsIgnoreCase(name))
				return entry.getValue();
		return null;
	}

	/**
	 * @return The format to write the image in
	 */
	public String getFormat()
	{
		return theFormat;
	}

	/**
	 * @return The type of WMS request this is
	 */
	public RequestType getRequest()
	{
		return theRequest;
	}

	/**
	 * @return The service specified by the request
	 */
	public String getService()
	{
		return theService;
	}

	/**
	 * @return This request's bounding box
	 */
	public BoundingBox getBounds()
	{
		return theBounds;
	}

	/**
	 * @return The width of the image to return (for {@link RequestType#Map}) or the image on the
	 *         client (for {@link RequestType#GetFeatureInfo})
	 */
	public int getWidth()
	{
		return theWidth;
	}

	/**
	 * @return The height of the image to return (for {@link RequestType#Map}) or the image on the
	 *         client (for {@link RequestType#GetFeatureInfo})
	 */
	public int getHeight()
	{
		return theHeight;
	}

	/**
	 * @return The SRS projection of the WMS request
	 */
	public String getSRS()
	{
		return theSRS;
	}

	/**
	 * @return The x-pixel to return feature info for
	 */
	public int getX()
	{
		return theSelectedX;
	}

	/**
	 * @return The y-pixel to return feature info for
	 */
	public int getY()
	{
		return theSelectedY;
	}

	/**
	 * @return The layers that were requested
	 */
	public String [] getLayers()
	{
		return theLayers;
	}

	/**
	 * @return The background requested for the image
	 */
	public Color getBackground()
	{
		return theBackgroundColor;
	}

	/**
	 * @return The transparent flag specified by the request, or false if unset
	 */
	public boolean isTransparent()
	{
		return isTransparent;
	}

	/**
	 * @param layerName The name of the layer to test
	 * @return Whether the given layer was requested by the client
	 */
	public boolean containsLayer(String layerName)
	{
		for(String layer : theLayers)
			if(layer.equalsIgnoreCase(layerName))
				return true;
		return false;
	}

	private static void fillCapabilities(PrismsWmsRequest ret)
	{
	}

	private static void fillFeatureInfo(PrismsWmsRequest ret)
	{
		fillMapInfo(ret);
		ret.theSelectedX = Integer.parseInt(ret.getParameter("x"));
		ret.theSelectedY = Integer.parseInt(ret.getParameter("y"));
	}

	private static void fillMap(PrismsWmsRequest ret)
	{
		fillMapInfo(ret);
	}

	private static void fillList(PrismsWmsRequest ret)
	{
	}

	private static void fillMapInfo(PrismsWmsRequest ret)
	{
		ret.theWidth = Integer.parseInt(ret.getParameter("width"));
		if(ret.theWidth > MAX_WIDTH)
			throw new IllegalArgumentException("WIDTH[" + ret.theWidth + "] larger than ["
				+ MAX_WIDTH + "]");

		ret.theHeight = Integer.parseInt(ret.getParameter("height"));
		if(ret.theHeight > MAX_HEIGHT)
			throw new IllegalArgumentException("HEIGHT[" + ret.theHeight + "] larger than ["
				+ MAX_HEIGHT + "]");

		String layers = ret.getParameter("layers");
		if(layers != null)
			ret.theLayers = layers.split(",");

		String BBOX = ret.getParameter("bbox");
		if(BBOX == null)
			throw new IllegalArgumentException("No BBOX was specified!");
		String [] bBoxSplit = BBOX.split(",");
		if(bBoxSplit.length != 4)
			throw new IllegalArgumentException("Illegal BBOX argument: " + BBOX
				+ "--not enough coords");
		float MinLon = Float.parseFloat(bBoxSplit[0]);
		float MinLat = Float.parseFloat(bBoxSplit[1]);
		float MaxLon = Float.parseFloat(bBoxSplit[2]);
		float MaxLat = Float.parseFloat(bBoxSplit[3]);
		if(MinLat > MaxLat)
			throw new IllegalArgumentException("Illegal BBOX: " + BBOX + "--minLat>maxLat");

		if(MinLon > MaxLon)
		{
			// ret.theError="Illegal BBOX: " + BBOX + "--minLon>maxLon";
			// return;
			MinLon = MinLon - 360f;
		}
		ret.theBounds = new BoundingBox(MinLat, MaxLat, MinLon, MaxLon);

		ret.theSRS = ret.getParameter("SRS");
		// if(ret.theSRS != null && !ret.theSRS.equalsIgnoreCase(EPSG_LATLON))
		// throw new IllegalArgumentException("Illegal SRS: Only " + EPSG_LATLON
		// + " is currently supported");

		ret.isTransparent = "false".equalsIgnoreCase(ret.getParameter("transparent"));
		Color bgColor = BG;
		String bgColorString = ret.getParameter("bgcolor");
		if(bgColorString != null)
			bgColor = java.awt.Color.decode(bgColorString);
		ret.theBackgroundColor = bgColor;
	}

	/**
	 * Inspects an HTTP parameter map for a property case-insensitively
	 * 
	 * @param paramMap The HTTP parameter map (from {@link HttpServletRequest#getParameterMap()})
	 * @param name The name of the parameter to get
	 * @return The first value of the paramter in the map
	 */
	public static String getParameter(Map<String, String []> paramMap, String name)
	{
		for(Map.Entry<String, String []> entry : paramMap.entrySet())
			if(entry.getKey().equalsIgnoreCase(name))
			{
				String [] value = entry.getValue();
				if(value.length == 0)
					return null;
				return value[0];
			}
		return null;
	}

	/**
	 * Prints an error to the response in WMS's application/vnd.ogc.se_xml format
	 * 
	 * @param response The response to print the error to
	 * @param error The error message to print
	 * @throws java.io.IOException If a problem occurs writing to the response
	 */
	public static void respondError(javax.servlet.http.HttpServletResponse response, String error)
		throws java.io.IOException
	{
		java.io.PrintWriter out = new java.io.PrintWriter(response.getOutputStream());

		response.setContentType("application/vnd.ogc.se_xml");

		out.println("<ServiceExceptionReport version=\"1.1.1\">");
		out.println("<ServiceException>");
		out.println(error + "\n\n");
		out.println("</ServiceException>");
		out.println("</ServiceExceptionReport>");
		out.close();
	}
}
