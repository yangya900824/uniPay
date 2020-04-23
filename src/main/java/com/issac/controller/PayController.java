package com.issac.controller;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.issac.config.SwiftpassConfig;
import com.issac.utils.MD5;
import com.issac.utils.SignUtils;
import com.issac.utils.XmlUtils;

@Controller
@RequestMapping("/pay")
public class PayController {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private SwiftpassConfig swiftpassConfig;

	@GetMapping("/toPay")
	public String pay(HttpServletRequest req, Model model) throws ServletException, IOException {
		log.info("支付请求...");
		// SortedMap<String, String> map = XmlUtils.getParameterMap(req);
		SortedMap<String, String> map = new TreeMap<String,String>();
		// {attach=附加信息, body=测试购买商品, mch_create_ip=123.12.12.123,
		// method=submitOrderInfo, out_trade_no=8891456782896485,
		// service=pay.weixin.jspay, total_fee=1}
		map.put("attach", "附加信息");
		//map.put("sub_openid", "oR0je5yxLoZatZvp2aOd60qdEZfA");
		//map.put("sub_appid", "wx3e968eac9c59390c");
		map.put("body", "测试购买商品");
		map.put("mch_create_ip", "123.12.12.123");
		map.put("method", "submitOrderInfo");
		map.put("out_trade_no", System.currentTimeMillis() + "");
		map.put("total_fee", "100");
		map.put("service", "pay.weixin.jspay");
		map.put("version", "2.0");
		map.put("charset", "UTF-8");
		map.put("sign_type", "MD5");
		map.put("is_raw", "1");
		map.put("mch_id", swiftpassConfig.getMch_id());
		map.put("notify_url", swiftpassConfig.getNotify_url());
		//map.put("sign_agentno", "");//机构号
		map.put("nonce_str", String.valueOf(new Date().getTime()));
/*//              {attach=附加信息, 
		        body=测试购买商品, 
				charset=UTF-8,
				is_raw=1, 
				mch_create_ip=123.12.12.123, 
				mch_id=7551000001, 
				method=submitOrderInfo, 
				nonce_str=1587606772714, 
				notify_url=http://xxx/jsapi_wx/testPayResult, 
				out_trade_no=3863357305756825, 
				service=pay.weixin.jspay, 
				sign=872F1044E3D0964496B465BA1503B818, 
				sign_type=MD5, 
				total_fee=1, 
				version=2.0}*/
		Map<String, String> params = SignUtils.paraFilter(map);
		StringBuilder buf = new StringBuilder((params.size() + 1) * 10);
		SignUtils.buildPayParams(buf, params, false);
		String preStr = buf.toString();
		String sign = MD5.sign(preStr, "&key=" + swiftpassConfig.getKey(), "utf-8");
		map.put("sign", sign);

		String reqUrl = swiftpassConfig.getReq_url();
		log.info("reqUrl：" + reqUrl);

		log.info("reqParams:" + XmlUtils.parseXML(map));
		CloseableHttpResponse response = null;
		CloseableHttpClient client = null;
		String res = null;
		Map<String, String> resultMap = null;
		try {
			HttpPost httpPost = new HttpPost(reqUrl);
			StringEntity entityParams = new StringEntity(XmlUtils.parseXML(map), "utf-8");
			httpPost.setEntity(entityParams);
			httpPost.setHeader("Content-Type", "text/xml;utf-8");
			client = HttpClients.createDefault();
			response = client.execute(httpPost);
			if (response != null && response.getEntity() != null) {
				resultMap = XmlUtils.toMap(EntityUtils.toByteArray(response.getEntity()), "utf-8");
				res = XmlUtils.toXml(resultMap);
				response.close();
				client.close();
				log.info("请求结果：" + res);
				if (!SignUtils.checkParam(resultMap, swiftpassConfig.getKey())) {
					res = "验证签名不通过";
				} else {
					if ("0".equals(resultMap.get("status")) && "0".equals(resultMap.get("result_code"))) {
						String pay_info = resultMap.get("pay_info");
						log.debug("pay_info : " + pay_info);
						JSONObject parseObject = JSONObject.parseObject(pay_info);
						model.addAttribute("appId", parseObject.getString("appId"));
						model.addAttribute("nonceStr", parseObject.getString("nonceStr"));
						model.addAttribute("packageValue", parseObject.getString("package"));
						model.addAttribute("paySign", parseObject.getString("paySign"));
						model.addAttribute("signType", parseObject.getString("signType"));
						model.addAttribute("timeStamp", parseObject.getString("timeStamp"));
						res = "ok";
						return "pay2";
					}
				}
			} else {
				res = "操作失败";
				return "pay";
			}
		} catch (Exception e) {
			log.info("操作失败，原因：", e);
			res = "系统异常";
		} 
		Map<String, String> result = new HashMap<String, String>();
		if ("ok".equals(res)) {
			result = resultMap;
		} else {
			result.put("status", "500");
			result.put("msg", res);
		}
		return "pay";
	}
	
	@GetMapping("/refund")
	 public void refund(String outTradeNo,String price, HttpServletResponse resp) throws ServletException, IOException{
	        log.debug("退款...");
	        SortedMap<String,String> map = new TreeMap<String,String>();//XmlUtils.getParameterMap(req);
	        map.put("service", "unified.trade.refund");
	        map.put("version", "2.0");
			map.put("charset", "UTF-8");
			map.put("sign_type", "MD5");
			
			map.put("out_trade_no", outTradeNo);
			map.put("out_refund_no", outTradeNo);
			map.put("total_fee", price);
			map.put("refund_fee", price);
	        
	        String key = swiftpassConfig.getKey();
	        String reqUrl = swiftpassConfig.getReq_url();
	        map.put("mch_id", swiftpassConfig.getMch_id());
	        map.put("op_user_id", swiftpassConfig.getMch_id());
	        map.put("sign_agentno", "");//机构号
	        map.put("nonce_str", String.valueOf(new Date().getTime()));
	        
	        Map<String,String> params = SignUtils.paraFilter(map);
	        StringBuilder buf = new StringBuilder((params.size() +1) * 10);
	        SignUtils.buildPayParams(buf,params,false);
	        String preStr = buf.toString();
	        String sign = MD5.sign(preStr, "&key=" + key, "utf-8");
	        map.put("sign", sign);
	        
	        log.debug("reqUrl:" + reqUrl);
	        
	        CloseableHttpResponse response = null;
	        CloseableHttpClient client = null;
	        String res = null;
	        try {
	            HttpPost httpPost = new HttpPost(reqUrl);
	            StringEntity entityParams = new StringEntity(XmlUtils.parseXML(map),"utf-8");
	            httpPost.setEntity(entityParams);
	            httpPost.setHeader("Content-Type", "text/xml;utf-8");
	            client = HttpClients.createDefault();
	            response = client.execute(httpPost);
	            if(response != null && response.getEntity() != null){
	                Map<String,String> resultMap = XmlUtils.toMap(EntityUtils.toByteArray(response.getEntity()), "utf-8");
	                res = XmlUtils.toXml(resultMap);
	                log.debug("请求结果：" + res);
	                
	                if(!SignUtils.checkParam(resultMap, key)){
	                    res = "验证签名不通过";
	                }
	            }else{
	                res = "操作失败!";
	            }
	        } catch (Exception e) {
	            log.error("操作失败，原因：",e);
	            res = "操作失败";
	        } finally {
	            if(response != null){
	                response.close();
	            }
	            if(client != null){
	                client.close();
	            }
	        }
	        Map<String,String> result = new HashMap<String,String>();
	        if(res.startsWith("<")){
	            result.put("status", "200");
	            result.put("msg", "操作成功，请在日志文件中查看");
	        }else{
	            result.put("status", "500");
	            result.put("msg", res);
	        }
	        resp.getWriter().write(new Gson().toJson(result));
	    }
	 
	
	@RequestMapping("/payNotic")
	public void payNotic(HttpServletRequest req,HttpServletResponse resp){
		try {
            log.debug("收到通知...");
            req.setCharacterEncoding("utf-8");
            resp.setCharacterEncoding("utf-8");
            resp.setHeader("Content-type", "text/html;charset=UTF-8");

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            String FEATURE = null;
            try {
            	FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
            	dbf.setFeature(FEATURE, true);
            	FEATURE = "http://xml.org/sax/features/external-general-entities";
            	dbf.setFeature(FEATURE, false);
            	FEATURE = "http://xml.org/sax/features/external-parameter-entities";
            	dbf.setFeature(FEATURE, false);
            	FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
            	dbf.setFeature(FEATURE, false);
            	dbf.setXIncludeAware(false);
            	dbf.setExpandEntityReferences(false);
            } catch (ParserConfigurationException e) {
            	System.out.println("ParserConfigurationException was thrown. The feature '" +
            	FEATURE + "' is probably not supported by your XML processor.");
            }
            String resString = XmlUtils.parseRequst(req);
            log.debug("请求的内容：" + resString);
            String respString = "error";
            if(resString != null && !"".equals(resString)){
                Map<String,String> map = XmlUtils.toMap(resString.getBytes(), "utf-8");
                String res = XmlUtils.toXml(map);
                log.debug("请求结果：" + res);
                if(map.containsKey("sign")){
                    if(!SignUtils.checkParam(map, swiftpassConfig.getKey())){
                        res = "验证签名不通过";
                        respString = "error";
                    }else{
                        String status = map.get("status");
                        if(status != null && "0".equals(status)){
                            String result_code = map.get("result_code");
                            if(result_code != null && "0".equals(result_code)){
                                respString = "success";
                            } 
                        } 
                    }
                }
            }
            resp.getWriter().write(respString);
        } catch (Exception e) {
           log.error("操作失败，原因：",e);
        }
		
	}
	
	@GetMapping("/refundQuery")
	 public void refundQuery(String outTradeNo, HttpServletResponse resp) throws ServletException, IOException{
	        log.debug("退款查询...");
	        SortedMap<String,String> map = new TreeMap<String,String>();//XmlUtils.getParameterMap(req);
	        map.put("service", "unified.trade.refundquery");
	        map.put("version", "2.0");
			map.put("charset", "UTF-8");
			map.put("sign_type", "MD5");
			map.put("out_trade_no", outTradeNo);
			
	        String key = swiftpassConfig.getKey();
	        String reqUrl = swiftpassConfig.getReq_url();
	        map.put("mch_id", swiftpassConfig.getMch_id());
	        map.put("nonce_str", String.valueOf(new Date().getTime()));
	        
	        Map<String,String> params = SignUtils.paraFilter(map);
	        StringBuilder buf = new StringBuilder((params.size() +1) * 10);
	        SignUtils.buildPayParams(buf,params,false);
	        String preStr = buf.toString();
	        String sign = MD5.sign(preStr, "&key=" + key, "utf-8");
	        map.put("sign", sign);
	        
	        log.debug("reqUrl:" + reqUrl);
	        
	        CloseableHttpResponse response = null;
	        CloseableHttpClient client = null;
	        String res = null;
	        try {
	            HttpPost httpPost = new HttpPost(reqUrl);
	            StringEntity entityParams = new StringEntity(XmlUtils.parseXML(map),"utf-8");
	            httpPost.setEntity(entityParams);
	            httpPost.setHeader("Content-Type", "text/xml;utf-8");
	            client = HttpClients.createDefault();
	            response = client.execute(httpPost);
	            if(response != null && response.getEntity() != null){
	                Map<String,String> resultMap = XmlUtils.toMap(EntityUtils.toByteArray(response.getEntity()), "utf-8");
	                res = XmlUtils.toXml(resultMap);
	                log.debug("请求结果：" + res);
	                
	                if(!SignUtils.checkParam(resultMap, key)){
	                    res = "验证签名不通过";
	                }
	            }else{
	                res = "操作失败!";
	            }
	        } catch (Exception e) {
	            log.error("操作失败，原因：",e);
	            res = "操作失败";
	        } finally {
	            if(response != null){
	                response.close();
	            }
	            if(client != null){
	                client.close();
	            }
	        }
	        Map<String,String> result = new HashMap<String,String>();
	        if(res.startsWith("<")){
	            result.put("status", "200");
	            result.put("msg", "操作成功，请在日志文件中查看");
	        }else{
	            result.put("status", "500");
	            result.put("msg", res);
	        }
	        resp.getWriter().write(new Gson().toJson(result));
	    }

}
