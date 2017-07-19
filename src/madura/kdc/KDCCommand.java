/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package madura.kdc;

import madura.exec.CallbackTimer;
import madura.exec.Callback;
import madura.debug.Log;
import java.util.Random;

/**
 *
 * @author lgerard
 */
public class KDCCommand {
	/***************************************************************************/
	/* ATTRIBUTS PRIVES */
	
	/**
	 * Commande à appeler
	 */
	private String command;
	
	/**
	 * Réponse apportée à la commande
	 */
	private String response;
	
	/**
	 * Commande appelée ?
	 */
	private boolean called = false;
	
	/**
	 * Accusé de reception ?
	 */
	private boolean returned = false;
	
	/**
	 * Commande en erreur ?
	 */
	private boolean error = false;
	
	/**
	 * Thread de contrôle de réception d'accusé de réception
	 */
	private CallbackTimer ctrlThread;
	
	/**
	 * Objet KDCCommunication lié
	 */
	private KDCCommunication com;
	
	/**
	 * Nombre de tentatives de transmission
	 */
	private int retryCount = 0;
	
	/**
	 * Nombre de tentatives MAX autorisées
	 */
	private int retryMax;
	
	/**
	 * Durée entre chaque tentative (en ms)
	 */
	private long retryTime;
	
	private double uid;

	
	/***************************************************************************/
	/* CONSTRUCTEUR */
	
	/**
	 * Constructeur
	 * @param String			cmd					Commande à appeler
	 * @param in_com			KDCCommunication	Objet KDCCommunication lié
	 * @param in_retryTime		long				Durée entre chaque tentative (en ms)
	 * @param in_retryMax		int					Nombre max de tentatives
	 */
	public KDCCommand(String cmd, KDCCommunication in_com, long in_retryTime, int in_retryMax) {
		command = cmd;
		com = in_com;
		retryTime = in_retryTime;
		retryMax = in_retryMax;
		uid = Math.random();
		
	}

	
	/***************************************************************************/
	/* METHODES CALLBACK */
	
	/**
	 * Event : Définit que la commande a été appelée
	 */
	public void setCalled() {
		called = true;
		if (command == "W") {
			ctrlThread = new CallbackTimer(retryTime, new Callback(this, "controlExecution"));
		}
	}
	
	/**
	 * Event : une réponse a été apportée
	 * @param String	in_response 
	 */
	public void setResponse(String in_response) {
		if (ctrlThread != null) {
			ctrlThread.interrupt();
		}
		returned = true;
		called = true;
		response = in_response;
	}

	/**
	 * Event : une erreur a été rencontrée
	 */
	public void setError() {
		if (ctrlThread != null) {
			ctrlThread.interrupt();
			ctrlThread = null;
		}
		returned = true;
		error = true;
	}

	/**
	 * Event : vérification de l'éxecution
	 */
	public void controlExecution() {
		if (called && !returned && ctrlThread != null) {
			retryCount++;
			if (retryCount == retryMax) {
				ctrlThread.interrupt();
				ctrlThread = null;
				com.cmdTimeout();
			} else {
				com.sendNext();
			}
		}
	}
	
	/***************************************************************************/
	/* METHODES PUBLIQUES */

	/**
	 * Retourne la commande
	 * @return String
	 */
	public String getCommand() {
		return command;
	}

	
}
