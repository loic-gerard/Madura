/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package madura.kdc;

import java.util.Enumeration;
import javax.comm.CommPortIdentifier;
import madura.debug.Log;
import madura.kdc.KDCCommunication;
import madura.exec.Callback;
import java.util.Vector;
import madura.exec.Callback;
import madura.exec.CallbackTimer;

/**
 *
 * @author lgerard
 */
public class KDCDiscoverer{
	/***************************************************************************/
	/* ATTRIBUTS PRIVES */
	
	private CallbackTimer cbClose;
	
	/**
	 * Ports à tester
	 */
	private Vector ports;
	
	/**
	 * Callback à executer lorsque le port est détecté
	 */
	private Callback onPortDetectedCallback;
	
	/**
	 * Callback à executer lorsque un port est réservé par un autre périphérique
	 */
	private Callback onPortReservedCallback;
	
	/**
	 * Callback à executer lorsque la connexion avec le port est prête
	 */
	private Callback onConnexionReadyCallback;
	
	/**
	 * Callback à executer lorsque un KDC est prêt
	 */
	private Callback onDeviceReadyCallback;
	
	/**
	 * Callback à executer lorsque une erreur survient lors de l'initialisation d'un KDC
	 */
	private Callback onDeviceErrorCallback;
	
	/**
	 * Callback à executer lorsque tous les ports ont été testés
	 */
	private Callback onEndedCallback;

	private Vector allowPorts;
	
	/***************************************************************************/
	/* CONSTRUCTEUR */
	
	/**
	 * Constructeur
	 */
	public KDCDiscoverer() {
		ports = new Vector();
		allowPorts = new Vector();
	}
	
	/***************************************************************************/
	/* METHODES PUBLIQUES */
	
	/**
	 * Lance la découverte
	 */
	public void start(){
		initPorts();
		startDetect();
	}

	
	
	/**
	 * Définit un callback à executer lorsque un port est détecté - méthode(String portCom)
	 * @param Object	in_target	Instance sur laquelle appeler la méthode
	 * @param String	in_method	Nom de la méthode à appeler
	 */
	public void setOnPortDetectedCallBack(Object in_target, String in_method){
		onPortDetectedCallback = new Callback(in_target, in_method);
	}
	
	/**
	 * Définit un callback à executer lorsque un port est déjà réservé - méthode(String portCom)
	 * @param Object	in_target	Instance sur laquelle appeler la méthode
	 * @param String	in_method	Nom de la méthode à appeler
	 */
	public void setOnPortReservedCallBack(Object in_target, String in_method){
		onPortReservedCallback = new Callback(in_target, in_method);
	}
	
	/**
	 * Définit un callback à executer lorsque une connexion sur un port est prête (mais kdc non testé) - méthode(String portCom)
	 * @param Object	in_target	Instance sur laquelle appeler la méthode
	 * @param String	in_method	Nom de la méthode à appeler
	 */
	public void setOnConnexionReadyCallBack(Object in_target, String in_method){
		onConnexionReadyCallback = new Callback(in_target, in_method);
	}
	
	/**
	 * Définit un callback à executer lorsque un KDC est pret - méthode(String portCom)
	 * @param Object	in_target	Instance sur laquelle appeler la méthode
	 * @param String	in_method	Nom de la méthode à appeler
	 */
	public void setOnDeviceReadyCallBack(Object in_target, String in_method){
		onDeviceReadyCallback = new Callback(in_target, in_method);
	}
	
	/**
	 * Définit un callback à executer lorsque une erreur survient - méthode(String portCom, String erreur)
	 * @param Object	in_target	Instance sur laquelle appeler la méthode
	 * @param String	in_method	Nom de la méthode à appeler
	 */
	public void setOnDeviceErrorCallBack(Object in_target, String in_method){
		onDeviceErrorCallback = new Callback(in_target, in_method);
	}
	
	/**
	 * Définit un callback à executer lorsque tous les devices ont été testés - méthode(Vector portsValides)
	 * @param Object	in_target	Instance sur laquelle appeler la méthode
	 * @param String	in_method	Nom de la méthode à appeler
	 */
	public void setOnEndedCallBack(Object in_target, String in_method){
		onEndedCallback = new Callback(in_target, in_method);
	}
	
	
	/***************************************************************************/
	/* METHODES EVENT */
	
	/**
	 * Event: lorsque la connexion au port est prete
	 * @param Object in_port (String)
	 */
	public void eventOnConnexionOpened(Object in_port){
		KDCCommunication port = (KDCCommunication)in_port;
		if(onConnexionReadyCallback != null){
			onConnexionReadyCallback.call(port.getPort());
		}
		checkIfAllTested();
	}
	
	
	/**
	 * Event: lorsque une erreur survient
	 * @param Object in_port (String)
	 * @param Object in_erreur (String) erreur
	 */
	public void eventOnDeviceError(Object in_port, Object in_erreur){
		KDCCommunication port = (KDCCommunication)in_port;
		String erreur = (String)in_erreur; 
		
		if(onDeviceErrorCallback != null){
			onDeviceErrorCallback.call(port.getPort(), erreur);
		}
		checkIfAllTested();
	}
	
	
	/**
	 * Event: lorsque le KDC est pret
	 * @param Object in_port (String)
	 */
	public void eventOnDeviceReady(Object in_port){
		KDCCommunication port = (KDCCommunication)in_port;
		allowPorts.add(port.getPort());
		
		if(onDeviceReadyCallback != null){
			onDeviceReadyCallback.call(port.getPort());
		}
		checkIfAllTested();
	}
	
	public void eventOnEnded(){
		Log.log("Finalisation... Wait...", "madura.kdc.discoverer.KDCDiscoverer");
		
		cbClose = new CallbackTimer(5000, new Callback(this, "eventOnDevicesClosed"));
		closeAllOpenedDevices();
		
	}
	
	public void eventOnDevicesClosed(){
		Log.log("Détection des devices terminée !", "madura.kdc.discoverer.KDCDiscoverer");
		
		if(onEndedCallback != null){
			onEndedCallback.call(allowPorts);
		}
	}
	
	/**
	 * Ferme toutes les connexions ouvertes
	 */
	public void closeAllOpenedDevices(){
		for (int i = 0; i < ports.size(); i++) {
			KDCCommunication tester = (KDCCommunication) ports.get(i);
			tester.close();
		}
	}
	
	/***************************************************************************/
	/* METHODES PRIVEES */
	
	/**
	 * Lance la découverte des ports
	 */
	private void initPorts(){
		Log.log("Début de détection de nouveaux devices KDC", "madura.kdc.discoverer.KDCDiscoverer");
		
		//On récupère les ports COM ouverts
		Enumeration portsCom = null;
		portsCom = CommPortIdentifier.getPortIdentifiers();
		
		
		
		//On parcourt les ports COM
		CommPortIdentifier portId = null;
		
		
		while(portsCom.hasMoreElements()) {
			portId = (CommPortIdentifier) portsCom.nextElement();
			
			boolean found = false;
				for (int j = 0; j < ports.size(); j++) {
				KDCCommunication device = (KDCCommunication) ports.get(j);
				if (device.getPort().equals(portId.getName())) {
					found = true;
				}
			}
			
			if(!found){
				if (!portId.isCurrentlyOwned() && portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {

					//Port disponible à tester
					Log.log("Port à tester : "+portId.getName(), "madura.kdc.discoverer.KDCDiscoverer");
					if(onPortDetectedCallback != null){
						onPortDetectedCallback.call(portId.getName());
					}

					KDCCommunication device = new KDCCommunication(portId.getName());
					device.setOnErrorCallBack(this, "setDeviceError");
					device.setOnReadyCallBack(this, "setDeviceReady");
					ports.add(device);
				}else if(portId.getPortType() == CommPortIdentifier.PORT_SERIAL){
					Log.log("Port non testé (déjà réservé) : "+portId.getName(), "madura.kdc.discoverer.KDCDiscoverer");
					if(onPortDetectedCallback != null){
						onPortDetectedCallback.call(portId.getName());
					}
					if(onPortReservedCallback != null){
						onPortReservedCallback.call(portId.getName());
					}
				}
			}
		}
	}
	
	/**
	 * Débute la détection des KDC
	 */
	private void startDetect(){
		for(int i = 0; i < ports.size(); i++){
			KDCCommunication tester = (KDCCommunication)ports.get(i);
			tester.setOnConnexionOpenedCallBack(this, "eventOnConnexionOpened");
			tester.setOnErrorCallBack(this, "eventOnDeviceError");
			tester.setOnReadyCallBack(this, "eventOnDeviceReady");
			Log.log("Debut du test du port : "+tester.getPort(), "madura.kdc.discoverer.KDCDiscoverer");
			tester.start();
		}
	}
	
	/**
	 * Teste si tous les KDC ont été testés
	 */
	private void checkIfAllTested(){
		Vector portsOk = new Vector();
		
		boolean all = true;
		for(int i = 0; i < ports.size(); i++){
			KDCCommunication tester = (KDCCommunication)ports.get(i);
			if(!tester.isTested()){
				all = false;
			}
			if(tester.isConnected()){
				portsOk.add(tester.getPort());
			}
		}
		
		if(all){
			
			
			String[] detected = new String[portsOk.size()];
			for(int i = 0; i < portsOk.size(); i++){
				detected[i] = (String)portsOk.get(i);
			}
			
			cbClose = new CallbackTimer(5000, new Callback(this, "eventOnEnded"));
		}
	}
}
