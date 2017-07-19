/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package madura.exec;

import java.lang.reflect.Method;
import madura.debug.Log;
import madura.exec.Callback;

public class CallbackTimer extends Thread {
	/**
	 * Durée d'attente (en ms)
	 */
	private long waitTime;
	
	/**
	 * Processus interrompu ?
	 */
	private boolean interrupt = false;
	
	/**
	 * Callback à executer
	 * 
	 */
	private Callback call;
	
	/**
	 * Indique la récurrence de l'appel
	 */
	private boolean recurrent = false;

	
	/***************************************************************************/
	/* CONSTRUCTEUR */
	
	/**
	 * Constructeur
	 * @param long		ms			Durée d'attente en ms
	 * @param Callback	cb			Objet Callback
	 * @param boolean	recurrent	récurrence de l'appel
	 */
	public CallbackTimer(long ms, Callback cb, boolean in_recurrent) {
		waitTime = ms;
		call = cb;
		recurrent = in_recurrent;
		start();
	}
	
	/**
	 * Constructeur
	 * @param long		ms			Durée d'attente en ms
	 * @param Callback	cb			Objet Callback
	 */
	public CallbackTimer(long ms, Callback cb) {
		waitTime = ms;
		call = cb;
		start();
	}

	
	/***************************************************************************/
	/* METHODES PUBLIQUES */
	
	/**
	 * Interrompt le processus
	 */
	public void interrupt() {
		interrupt = true;
	}

	
	/**
	 * Méthode Thread start
	 */
	public void run() {
		while(!interrupt){
			try {
				Thread.sleep(waitTime);
			} catch (Exception e) {
			}
			if (!interrupt) {
				call.call();
			}
			if(!recurrent){
				interrupt = true;
			}
		}
	}
}
