/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package egovframework.hyb.mbl.pus.service.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

import egovframework.com.cmm.service.EgovProperties;
import egovframework.hyb.mbl.pus.service.EgovPushDeviceAPIService;
import egovframework.hyb.mbl.pus.service.PushDeviceAPIDefaultVO;
import egovframework.hyb.mbl.pus.service.PushDeviceAPIVO;
import egovframework.rte.fdl.cmmn.EgovAbstractServiceImpl;
import javapns.communication.ConnectionToAppleServer;
import javapns.communication.exceptions.CommunicationException;
import javapns.communication.exceptions.KeystoreException;
import javapns.devices.Device;
import javapns.devices.implementations.basic.BasicDevice;
import javapns.notification.AppleNotificationServerBasicImpl;
import javapns.notification.PushNotificationManager;
import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotification;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
/**  
 * @Class Name : EgovPushDeviceAPIServiceImpl.java
 * @Description : EgovPushDeviceAPIServiceImpl Class
 * @Modification Information  
 * @
 * @  ?????????       ?????????                   ????????????
 * @ ---------   ---------   -------------------------------
 * @ 2016.06.20   ?????????                   ????????????
 * @ 2017.02.15   ?????????        ???????????????(ES)-?????? ???????????? ?????? ?????? ??????[CWE-209]
 * 
 * @author ???????????? API ???????????? ?????????
 * @since 2016. 06.20
 * @version 1.0
 * @see
 * 
 *  Copyright (C) by Ministry of Interior All right reserved.
 */

@Service("EgovPushDeviceAPIService")
public class EgovPushDeviceAPIServiceImpl extends EgovAbstractServiceImpl implements EgovPushDeviceAPIService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EgovPushDeviceAPIServiceImpl.class);

	private static String   APNS_CERTIFICATE_PATH	= "C:/eclipse-jee-mars-2-win32-x86_64/egov_push_test_cert.p12";
	private static String   APNS_CERTIFICATE_PWD	= "egov1234";      // ?????? ????????? ????????????
	private static String   APNS_DEV_HOST	= "gateway.sandbox.push.apple.com"; // ????????? ?????? ???????????? 
	private static String   APNS_HOST		= "gateway.push.apple.com";  // ?????? ?????? ????????????
	private static int      APNS_PORT		= 2195;      // ??????..????????? ????????? ???????????? ???????????????
	private static int   	APNS_BADGE		= 1;       // App ????????? ??????????????? ????????? ??????
	private static String	APNS_SOUND		= "default";      // ?????? ?????????
	
	private static String	GCM_SERVER_API_KEY	= "AIzaSyD1kFG9Z3Kw-rvQgCl-0t4idpDetYE3UYM"; // GCM Server API Key
	
	
	/** PushAPIDAO */
    @Resource(name="PushDeviceAPIDAO")
    private PushDeviceAPIDAO pushDeviceAPIDAO;

	/**
	 * ?????? ?????? ????????? ????????????.
	 * @param vo - ????????? ????????? ?????? PushDeviceAPIVO
	 * @return ?????? ??????
	 * @exception Exception
	 */

/*    public int insertVibrator(PushDeviceAPIVO vo) throws Exception {
    	LOGGER.debug(vo.toString());
    	
    	return (Integer)PushDeviceAPIVO.insertVibrator(vo);    	
    }
    */
    /**
	 * ?????? ?????? ?????? ????????? ????????????.
	 * @param VO - ????????? ????????? ?????? VibratorAPIVO
	 * @return ?????? ?????? ?????? ??????
	 * @exception Exception
	 */
    public List<?> selectPushDeviceList(PushDeviceAPIDefaultVO searchVO) throws Exception {
        return pushDeviceAPIDAO.selectPushDeviceList(searchVO);
    }
    
	/**
	 * Push Notification??? ?????? Device ????????? ????????????.
	 * @param vo - ????????? ????????? ?????? PushAPIVO
	 * @return ?????? ??????
	 * @exception Exception
	 */
    public int insertPushDevice(PushDeviceAPIVO vo) throws Exception {
    	LOGGER.debug(vo.toString());
    	
    	return (Integer)pushDeviceAPIDAO.insertPushDevice(vo);
    }
    
	/**
	 * Push Notification??? ????????? ????????????.
	 * @param vo - ????????? ????????? ?????? PushAPIVO
	 * @return ?????? ??????
	 * @exception Exception
	 */
    public int insertPushInfo(PushDeviceAPIVO vo) throws Exception {
    	LOGGER.debug(vo.toString());
    	
    	// Android GCM, iOS APNS ?????? ???????????? ????????????.
    	if (vo.getOsType().contains("Android")) {
    		sendFCMAndroid(vo.getTokenId(), vo.getMessage());
    	} else {
    		sendAPNSPush(vo.getTokenId(), vo.getMessage());
    	}

    	return (Integer)pushDeviceAPIDAO.insertPushInfo(vo);    	
    }

    public int sendAPNSPush(String tokenId, String pushMessage) {

    	ArrayList<String> myTokens = new ArrayList<String>();
		myTokens.add(tokenId);
		HashMap<String, Object> map_pushInfo = new HashMap<String, Object>();
		map_pushInfo.put("sender_nick", "egovDeviceAPI");
		map_pushInfo.put("msg", pushMessage);
		
		int result = sendPushIOS(myTokens, map_pushInfo);

    	return result;
    }
    
	public int sendPushIOS(ArrayList<String> tokens, HashMap<String, Object> map_pushInfo){
		  // ????????? ???????????? - ?????? ?????????????????? 45?????? (????????? + ??????)
		 
		  int result = 0;
		 
		  // 1. ?????? ???????????? ?????????
		 
		  String sender_nick  = (String)map_pushInfo.get("sender_nick");
		  String msg   = (String)map_pushInfo.get("msg");
		  
		  // 2. ??????????????? or ??????????????? ??????
		   boolean single_push = true; 
		   if(tokens.size()==1){
		    single_push = true;    
		   }else{
		    single_push = false;
		   }
		 
		   try{
		    // 3. ?????? ????????? ??????
		    PushNotificationPayload payLoad = new PushNotificationPayload();
		    JSONObject jo_body = new JSONObject();
		    JSONObject jo_aps = new JSONObject();
		    JSONArray ja = new JSONArray();
		    jo_aps.put("alert",msg);
		    jo_aps.put("badge",APNS_BADGE);
		    jo_aps.put("sound",APNS_SOUND);
		    jo_aps.put("content-available",1);
		    
		    jo_body.put("aps",jo_aps);
		    jo_body.put("sender_nick",sender_nick);
		    payLoad = payLoad.fromJSON(jo_body.toString());
		    
		    PushNotificationManager pushManager = new PushNotificationManager();
		             pushManager.initializeConnection(
		               new AppleNotificationServerBasicImpl(APNS_CERTIFICATE_PATH, APNS_CERTIFICATE_PWD,ConnectionToAppleServer.KEYSTORE_TYPE_PKCS12, APNS_DEV_HOST, APNS_PORT));
		             
		             List<PushedNotification> notifications = new ArrayList<PushedNotification>();
		             
		             if (single_push){
		              // ??????????????? ?????? ??????
		                Device device = new BasicDevice();
		                device.setToken(tokens.get(0));
		                PushedNotification notification = pushManager.sendNotification(device, payLoad);
		                notifications.add(notification);
		             }else{
		              // ??????????????? ?????? ??????
		                 List<Device> device = new ArrayList<Device>();
		                 for (String token : tokens){
		                     device.add(new BasicDevice(token));
		                 }
		                 notifications = pushManager.sendNotifications(payLoad, device);
		             }
		              List<PushedNotification> failedNotifications = PushedNotification.findFailedNotifications(notifications);
		              List<PushedNotification> successfulNotifications = PushedNotification.findSuccessfulNotifications(notifications);
		              int failed = failedNotifications.size();
		              int successful = successfulNotifications.size();
		              if(failed > 0){
		               result = 1; // ?????? ????????? ??????
		              }else{
		               result = 0; // ?????? ?????? ??????
		              }
		         }catch(KeystoreException ke){
		          result = 2;
		          LOGGER.error("[KeystoreException] Try/Catch...KeystoreException : " + ke.getMessage());
		         }catch(CommunicationException ce){
		          result = 3;
		          LOGGER.error("[CommunicationException] Try/Catch...CommunicationException : " + ce.getMessage());
		         }catch (Exception e) {
		          result = 4;
		          //2017-02-15 ????????? ???????????????(ES)-?????? ???????????? ?????? ?????? ??????[CWE-209] 251-251
		          //e.printStackTrace();
		          LOGGER.error("["+e.getClass()+"] PushNotification : " + e.getMessage());
		         }
		  return result;
		 }
	
	/**
	 * Push Notification ?????? ?????? ????????? ??????.
	 * @param vo - ????????? ????????? ?????? PushDeviceAPIVO
	 * @return ?????? ??????
	 * @exception Exception
	 */
    public PushDeviceAPIVO selectPushDevice(PushDeviceAPIVO vo) throws Exception {
        return pushDeviceAPIDAO.selectPushDevice(vo);
    }
    
	/**
	 * Push Notification ?????? ????????? ????????? ??????.
	 * @param vo - ????????? ????????? ?????? PushDeviceAPIVO
	 * @return ?????? ??????
	 * @exception Exception
	 */
    public List<?> selectPushMessageList(PushDeviceAPIVO VO) throws Exception {
        return pushDeviceAPIDAO.selectPushMessageList(VO);
    }

    /**
     * Push Notification ????????? ????????? ????????? ????????? ??????.
     * @param VO
     * @return
     * @throws Exception
     */
    public int selectPushDeviceCount(PushDeviceAPIVO vo) throws Exception {
    	return pushDeviceAPIDAO.selectPushDeviceCount(vo);
    }

    /**
     * ??????????????? ?????? ??????
     * @param pushRegId
     * @param pushMessage
     * @return
     */
    public int sendFCMAndroid(String pushRegId, String pushMessage) {

    	int result = 0;

    	try {

	    	// AccessKey  ??????
	    	String PATH = EgovProperties.getPathProperty("Globals.fcmSdk");
	    	String MESSAGING_SCOPE = EgovProperties.getProperty("Globals.fcmMessagingScope");
	    	String[] SCOPES = { MESSAGING_SCOPE };
	    	GoogleCredential googleCredential = GoogleCredential.fromStream(new FileInputStream(PATH)).createScoped(Arrays.asList(SCOPES));
	    	googleCredential.refreshToken();

	    	// HTTP ????????? AccessKey ??????
	    	HttpHeaders headers = new HttpHeaders();
	    	headers.add("Authorization", "Bearer " + googleCredential.getAccessToken());
	    	headers.add("content-type", MediaType.APPLICATION_JSON_VALUE);

	    	JSONObject notification = new JSONObject();
	    	notification.put("title", "FCM Test Title"); // Push ??????
	    	notification.put("body", pushMessage); // Push ??????

	    	JSONObject message = new JSONObject();
	    	message.put("token", pushRegId); // ???????????? ??????
	    	message.put("notification", notification);

	    	JSONObject jsonParams = new JSONObject();
	    	jsonParams.put("message", message);

	    	HttpEntity<JSONObject> httpEntity = new HttpEntity<JSONObject>(jsonParams, headers);
	    	RestTemplate rt = new RestTemplate();

	    	// ????????? ??????(.../projects/{????????? ???????????? ID ??????}/messages...)
	    	ResponseEntity<String> res = rt.exchange(
		    	EgovProperties.getProperty("Globals.fcmRequestPath")
		    	, HttpMethod.POST
		    	, httpEntity
		    	, String.class
	    	);

	    	if (res.getStatusCode() != HttpStatus.OK) {
		    	result = 1;
	    	} else {
		    	result = 0;
	    	}

    	} catch (FileNotFoundException fnfe) {
    		LOGGER.error("["+fnfe.getClass()+"] FileNotFoundException : " + fnfe.getMessage());
    	} catch (IOException ioe) {
    		LOGGER.error("["+ioe.getClass()+"] IOException : " + ioe.getMessage());
    	} catch (Exception e) {
    		LOGGER.error("["+e.getClass()+"] Exception : " + e.getMessage());
    	}

    	return result;
    }

}
