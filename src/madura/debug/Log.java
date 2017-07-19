/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package madura.debug;

import java.util.Date;
import madura.exec.Callback;
import java.util.Vector;

/**
 *
 * @author lgerard
 */
public class Log {
	/***************************************************************************/
	/* ATTRIBUTS PRIVES
	
	/**
	 * Définit si le logging est activé
	 */
	private static boolean enabled = true;
	
	/**
	 * Callbacks enregistrés
	 */
	private static Vector callbacks;
	
	
	/***************************************************************************/
	/* METHODE STATIQUES PUBLIQUES

	/**
	 * Ecrit un log
	 * @param String	message		Message à écrire
	 * @param String	classe		Classe effectuant l'appel
	 */
	public static void log(String message, String classe) {
		if(enabled){
			Date date = new Date();
			message = date.toString() + " : " + message;

			System.out.println(message);
			
			if(callbacks != null){
				for(int i = 0; i < callbacks.size(); i++){
					Callback cb = (Callback)callbacks.get(i);
					cb.call(message, classe);
				}
			}
		}
	}
	
	/**
	 * Ecrit un log
	 * @param String	message		Message à écrire
	 */
	public static void log(String message) {
		log(message, "");
	}
	
	/**
	 * Active / désactive les logs
	 * @param boolean	etat		Active / désactive
	 */
	public static void setEnabled(boolean etat){
		enabled = etat;
	}
	
	
	/**
	 * Retourne l'état d'activation du logging
	 * @return boolean
	 */
	public static boolean isEnabled(){
		return enabled;
	}
	
	/**
	 * Ajoute un callBack(Object message, Object classe) appelé après l'envoi d'un nouveau message dans les logs
	 * @param in_target Objet cible
	 * @param in_method Méthode appelée
	 */
	public static void addOnLogCallBack(Object in_target, String in_method){
		if(callbacks == null){
			callbacks = new Vector();
		}
		callbacks.add(new Callback(in_target, in_method));
	}
}
