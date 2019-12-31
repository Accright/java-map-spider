package com.accright.plugins.spider.utils;

/**
 * 根据开源js脚本编写，源码地址：https://github.com/wandergis/coordTransform
 * @author ranchl
 *
 */
public class CoordinateTransform {
	private static Double x_PI = 3.14159265358979324 * 3000.0 / 180.0;
	private static Double PI = 3.1415926535897932384626;
	private static Double a = 6378245.0;
	private static Double ee = 0.00669342162296594323;

	/**
	 * 百度坐标系 (BD-09) 与 火星坐标系 (GCJ-02)的转换 即 百度 转 谷歌、高德
	 * 
	 * @param bd_lon
	 * @param bd_lat
	 * @returns [lng, lat]
	 */
	public static Double[] bd09togcj02(Double bd_lon, Double bd_lat) {
		Double x = bd_lon - 0.0065;
		Double y = bd_lat - 0.006;
		Double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_PI);
		Double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_PI);
		Double gg_lng = z * Math.cos(theta);
		Double gg_lat = z * Math.sin(theta);
		Double[] result = { gg_lng, gg_lat };
		return result;
	};

	/**
	 * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换 即谷歌、高德 转 百度
	 * 
	 * @param lng
	 * @param lat
	 * @returns [lng, lat]
	 */
	public static Double[] gcj02tobd09(Double lng, Double lat) {
		Double z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * x_PI);
		Double theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * x_PI);
		Double bd_lng = z * Math.cos(theta) + 0.0065;
		Double bd_lat = z * Math.sin(theta) + 0.006;
		Double[] result = { bd_lng, bd_lat };
		return result;
	};

	/**
	 * WGS84转GCj02
	 * 
	 * @param lng
	 * @param lat
	 * @returns [lng, lat]
	 */
	public static Double[] wgs84togcj02(Double lng, Double lat) {
		if (out_of_china(lng, lat)) {
			Double[] result = { lng, lat };
			return result;
		} else {
			Double dlat = transformlat(lng - 105.0, lat - 35.0);
			Double dlng = transformlng(lng - 105.0, lat - 35.0);
			Double radlat = lat / 180.0 * PI;
			Double magic = Math.sin(radlat);
			magic = 1 - ee * magic * magic;
			Double sqrtmagic = Math.sqrt(magic);
			dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * PI);
			dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * PI);
			Double mglat = lat + dlat;
			Double mglng = lng + dlng;
			Double[] result = { mglng, mglat };
			return result;
		}
	};

	/**
	 * GCJ02 转换为 WGS84
	 * 
	 * @param lng
	 * @param lat
	 * @returns [lng, lat]
	 */
	public static Double[] gcj02towgs84(Double lng, Double lat) {
		if (out_of_china(lng, lat)) {
			Double[] result = { lng, lat };
			return result;
		} else {
			Double dlat = transformlat(lng - 105.0, lat - 35.0);
			Double dlng = transformlng(lng - 105.0, lat - 35.0);
			Double radlat = lat / 180.0 * PI;
			Double magic = Math.sin(radlat);
			magic = 1 - ee * magic * magic;
			Double sqrtmagic = Math.sqrt(magic);
			dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * PI);
			dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * PI);
			Double mglat = lat + dlat;
			Double mglng = lng + dlng;
			Double[] result = { lng * 2 - mglng, lat * 2 - mglat };
			return result;
		}
	};

	private static double transformlat(double lng, double lat) {
		double ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
		ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0;
		ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0;
		return ret;
	};

	private static double transformlng(double lng, double lat) {
		double ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
		ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0;
		ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0;
		return ret;
	};

	/**
	 * 判断是否在国内，不在国内则不做偏移
	 * 
	 * @param lng
	 * @param lat
	 * @returns boolean}
	 */
	private static Boolean out_of_china(Double lng, Double lat) {
		// 纬度3.86~53.55,经度73.66~135.05
		return !(lng > 73.66 && lng < 135.05 && lat > 3.86 && lat < 53.55);
	};

	public static void main(String[] args) {
		Double[] result = CoordinateTransform.bd09togcj02(26.2, 106.3);
		System.out.println(result[1].toString() + "," + result[0].toString());
	}
}
