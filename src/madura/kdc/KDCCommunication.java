/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package madura.kdc;

import com.sun.comm.Win32Driver;
import javax.comm.SerialPort;
import javax.comm.CommDriver;
import java.io.BufferedReader;
import java.io.OutputStream;
import javax.comm.CommPortIdentifier;
import javax.comm.CommPort;
import java.io.InputStreamReader;
import java.util.Vector;
import madura.debug.Log;
import madura.exec.Callback;

public class KDCCommunication extends Thread {
	/***************************************************************************/
	/* ATTRIBUTS PRIVES */
	
	/**
	 * Nom du port COM
	 */
	private String port;
	
	/**
	 * Identifiant du port COM
	 */
	private CommPortIdentifier portIdentifier;
	
	/**
	 * Objet port série
	 */
	private SerialPort serialPort;
	
	/**
	 * Fluix de lecture
	 */
	private BufferedReader inStream;
	
	/**
	 * Flux d'écriture
	 */
	private OutputStream outStream;

	/**
	 * Liste des commandes à executer
	 */
	private Vector commandStack;

	/**
	 * Témoin de connexion effective du KDC (si TRUE : matériel reconnu)
	 */
	private boolean connected = false;
	
	/**
	 * Témoin de test du matériel
	 */
	private boolean tested = false;

	/**
	 * Témoin de vérouillage (locké si nouvel envoi de commande en cours)
	 */
	private boolean locked = false;

	/**
	 * Chaine lue dans le buffer
	 */
	private String readed = "";			//Chaine lue du buffer.

	/**
	 * Témoin de première initialisation (si TRUE la connexion doit être ouverte)
	 */
	private boolean toOpen = true;

	/**
	 * Témoin de lecture batterie (à refondre)
	 */
	private boolean nextBat = true;
	
	/**
	 * Callback à l'ouverture de la connexion sur le port COM
	 */
	private Callback onConnexionOpenedCallBack = null;
	
	/**
	 * Callback lorsque le matériel KDC est pret à être utilisé
	 */
	private Callback onReadyCallBack = null;
	
	/**
	 * Callback lorsqu'une erreur est rencontrée lors de l'initialisation du matériel
	 */
	private Callback onErrorCallBack = null;
	
	/**
	 * Callback quand un message est reçu
	 */
	private Callback onMessageReadedCallBack = null;

	
	/***************************************************************************/
	/* Constructeur */
	
	/**
	 * Constructeur
	 * @param String	portCOM		Port COM à utiliser
	 */
	public KDCCommunication(String portCOM) {
		//Initialisation des variables
		port = portCOM;
		commandStack = new Vector();
	}
	
	/***************************************************************************/
	/* METHODES PUBLIQUES */
	
	/**
	 * Définit la méthode appelée lorsque la connexion au port est ouverte  -> methode(KDCCommunication caller)
	 * @param Object	in_target	Instance sur laquelle appeler la méthode de callback
	 * @param String	in_method	Nom de la méthode à appeler
	 */
	public void setOnConnexionOpenedCallBack(Object in_target, String in_method){
		onConnexionOpenedCallBack = new Callback(in_target, in_method);
	}
	
	/**
	 * Définit la méthode appelée lorsque le KDC est pret  -> methode(KDCCommunication caller)
	 * @param Object	in_target	Instance sur laquelle appeler la méthode de callback
	 * @param String	in_method	Nom de la méthode à appeler
	 */
	public void setOnReadyCallBack(Object in_target, String in_method){
		onReadyCallBack = new Callback(in_target, in_method);
	}
	
	/**
	 * Définit la méthode appelée lorsque une erreur survient lors de l'initialisation -> methode(KDCCommunication caller, String details)
	 * @param Object	in_target	Instance sur laquelle appeler la méthode de callback
	 * @param String	in_method	Nom de la méthode à appeler
	 */
	public void setOnErrorCallBack(Object in_target, String in_method){
		onErrorCallBack = new Callback(in_target, in_method);
	}
	
	/**
	 * Définit la méthode appelée lorsqu'un message est reçu  -> methode(KDCCommunication caller, String message)
	 * @param Object	in_target	Instance sur laquelle appeler la méthode de callback
	 * @param String	in_method	Nom de la méthode à appeler
	 */
	public void setOnMessageReadedCallBack(Object in_target, String in_method){
		onMessageReadedCallBack = new Callback(in_target, in_method);
	}
	
	/**
	 * Retourne si le KDC est connecté
	 * @return boolean
	 */
	public boolean isConnected(){
		return connected;
	}
	
	/**
	 * Retourne si le KDC a été testé
	 * @return boolea
	 */
	public boolean isTested(){
		return tested;
	}
	
	/**
	 * Ferme la connexion au KDC
	 */
	public void close() {
		log("Fermeture de la connexion au KDC...");
		connected = false;
		
		for(int i = 0; i < commandStack.size(); i++){
			KDCCommand cmd = (KDCCommand) commandStack.get(i);
			cmd.setError();
		}
		
		try{
		outStream.flush();
		serialPort.disableReceiveTimeout();
		serialPort.removeEventListener();
		serialPort.close();
		outStream = null;
		inStream = null;
		}catch(Exception e){}
		
		try{
			this.interrupt();
			this.stop();
		}catch(Exception e){
		}
		
	}

	/**
	 * Enregistre une commande pour l'envoi
	 * @param String	cmd	Commande à envoyer 
	 */
	public void sendCommand(String cmd) {
		log("Envoi d'une commande : " + cmd);
		saveCommand("W");
		saveCommand(cmd);
		sendNext();
	}
	
	/**
	 * Retourne le nom du port
	 * @return String
	 */
	public String getPort() {
		return port;
	}

	/**
	 * Envoi d'un message destiné à l'écran du KDC
	 * @param String line1 Ligne à afficher
	 */
	public void kdcCmd_message(String line1) {
		int charsByLine = 4;
		String bline1 = "";
		for (int i = 0; i < charsByLine; i++) {
			if (line1.length() >= (i + 1)) {
				bline1 += line1.charAt(i);
			} else {
				bline1 += " ";
			}
		}

		sendCommand("GMT" + bline1 + "@");
	}

	/**
	 * Allume la lumière rouge du KDC
	 */
	public void kdcCmd_redLight() {
		kdcCmd_clearLights();
		sendCommand("GML3#@");
	}

	/**
	 * Allume la lumière orange du KDC
	 */
	public void kdcCmd_orangeLight() {
		sendCommand("GML5#@");
	}

	/**
	 * Allume la lumière verte du KDC
	 */
	public void kdcCmd_greenLight() {
		kdcCmd_clearLights();
		sendCommand("GML1#@");
	}

	/**
	 * Eteint les lumières du KDC
	 */
	public void kdcCmd_clearLights() {
		sendCommand("GML4#@");
	}

	/**
	 * Le KDC emet un bip d'erreur
	 */
	public void kdcCmd_errorBeep() {
		sendCommand("GMB0@");
	}

	/**
	 * Le KDC emet un bip d'OK
	 */
	public void kdcCmd_okBeep() {
		sendCommand("GMB1@");
	}

	/***************************************************************************/
	/* METHODES PRIVEES */
	
	/**
	 * Ouvre la connexion au KDC
	 */
	private void open() {
		serialPort = null;
		portIdentifier = null;
		inStream = null;
		outStream = null;

		log("Debut d'initialisation du KDC");

		String driverName = "com.sun.comm.Win32Driver";
		try {
			CommDriver commdriver = (CommDriver) Class.forName(driverName).newInstance();
			commdriver.initialize();
		} catch (Exception e) {
			e.printStackTrace();
			if(onErrorCallBack != null){
				onErrorCallBack.call(this, "Erreur d'initialisation du driver COM");
			}
			return;
		}

		//initialisation du driver
		log("Initialisation du driver portCOM Windows...");
		Win32Driver w32Driver = new Win32Driver();
		w32Driver.initialize();

		//récupération de l'identifiant du port
		log("Recuperation de l'identifiant du port...");
		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(port);
		} catch (Exception e) {
			if(onErrorCallBack != null){
				onErrorCallBack.call(this, "Erreur de récupération des données du port COM");
			}
			tested = true;
			e.printStackTrace();
			return;
		}

		//ouverture du port
		log("Ouverture du port...");


		while (serialPort == null) {
			try {
				serialPort = (SerialPort) portIdentifier.open("KDC-" + port, 10000);
			} catch (Exception e) {
				e.printStackTrace();
				tested = true;
				if (onErrorCallBack != null) {
					onErrorCallBack.call(this, "Erreur d'ouverture du port COM");
				}
				return;
				/*
				e.printStackTrace();
				try {
					Thread.sleep(500);
				} catch (Exception e2) {
					tested = true;
					if(onErrorCallBack != null){
						onErrorCallBack.call(this, "Erreur d'ouverture du port COM");
					}
				}*/
			}
			
		}

		//règle les paramètres de la connexion
		log("Réglage des paramètres de la connexion du port : WAIT");

		try {
			serialPort.setSerialPortParams(
					9600,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
		} catch (Exception e) {
			tested = true;
			if(onErrorCallBack != null){
				onErrorCallBack.call(this, "Erreur de paramétrage du port COM");
			}
			e.printStackTrace();
			return;
		}

		//récupération du flux de lecture et écriture du port
		log("Ouverture des flux de lecture/ecriture sur port...");
		try {
			outStream = serialPort.getOutputStream();
			inStream = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
		} catch (Exception e) {
			tested = true;
			if(onErrorCallBack != null){
				onErrorCallBack.call(this, "Erreur d'ouverture des flux de lecture/écriture sur le port COM");
			}
			e.printStackTrace();
			return;
		}

		log("Connexion au port COM du KDC effectuée avec succès !");

		if(onConnexionOpenedCallBack != null){
			onConnexionOpenedCallBack.call(this);
		}
		
		saveCommand("W");
		sendNext();
	}

	/**
	 * Enregistre une commande
	 * @param String	cmd		Commande 
	 */
	private void saveCommand(String cmd) {
		log("Enregistrement d'une commande : " + cmd);
		KDCCommand c = new KDCCommand(cmd, this, 1000, 10);
		commandStack.add(c);
	}
	
	/**
	 * Met en pause le processus
	 * @param long	ms	Durée en ms 
	 */
	private void pause(long ms) {
		try {
			Thread.sleep(ms);
		} catch (Exception e) {
		}
	}

	/**
	 * Enregistre un log de traitement
	 * @param String	message		Message à enregistrer
	 */
	private void log(String message) {
		Log.log(port + " : " + message, this.getClass().toString());
	}
	
	
	/***************************************************************************/
	/* METHODES CALLBACK */

	/**
	 * Commande en timeout
	 */
	public void cmdTimeout() {
		log("Commande en timeout !");
		
		if(!connected){
			tested = true;
			if(onErrorCallBack != null){
				onErrorCallBack.call(this, "Commande d'initialisation en Timeout !");
			}
		}
	}
	
	/**
	 * Envoi la commande suivante
	 */
	public void sendNext() {
		
		if (!locked && commandStack.size() > 0) {
			KDCCommand c = (KDCCommand) commandStack.firstElement();

			try {
				log(c.getCommand() + " : STARTED");
				locked = true;

				String cmd = c.getCommand();

				//Démarrage d'un Thread de contrôle et de renvoi de la commande W au besoin.
				if (cmd == "W") {
					c.setCalled();
				}

				for (int i = 0; i < cmd.length(); i++) {
					char ctosend = cmd.charAt(i);
					if (ctosend == '@') {
						ctosend = 13;
					}
					log("Envoyé dans le flux de sortie : " + ctosend);
					locked = false;
					outStream.write((int) ctosend);

					if (i == (cmd.length() - 1)) {
						log(c.getCommand() + " : en attente d'une réponse du KDC");
					} else {
						pause(20);
					}
				}

				
			} catch (Exception e) {
				log(c.getCommand() + " : erreur !");
				e.printStackTrace();
			}
		}
	}

	/***************************************************************************/
	/* METHODES Thread */

	/**
	 * run()
	 */
	public void run() {
		if (toOpen) {
			toOpen = false;
			open();
		}

		try {
			while (1 == 1) {
				int c = inStream.read();
				char ca = (char) c;

				if (commandStack.size() > 0) {
					if (ca == '@') {
						KDCCommand command = (KDCCommand) commandStack.firstElement();
						log(command.getCommand() + " : commande OK avec comme réponse : " + readed);
						if (nextBat && readed.length() > 0) {
							nextBat = false;
							int asciival = (int) readed.charAt(0);

							log("Etat batterie : " + asciival + "%");
						}
						command.setResponse(readed);
						readed = "";

						//Si première connexion. KDC bien reconnu
						if (!connected) {
							tested = true;
							connected = true;
							if(onReadyCallBack != null){
								onReadyCallBack.call(this);
							}
						}

						commandStack.remove(0);
						sendNext();
					} else if (ca == '!') {
						KDCCommand command = (KDCCommand) commandStack.firstElement();
						log(command.getCommand() + " : commande non reconnue !");
						command.setError();
						readed = "";

						commandStack.remove(0);
						sendNext();
					} else {
						int asciival = (int) ca;

						if (asciival == 0) {
							nextBat = true;
						} else {
							readed += ca;
						}
					}
				} else {

					if (ca != 13 && ca != 10 && ca != 64) {
						readed += ca;
					}

					if (ca == 13) {
						log("CODE BARRE LU : " + readed);
						if(onMessageReadedCallBack != null){
							onMessageReadedCallBack.call(this, readed);
						}
						readed = "";
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	

	
	

}
