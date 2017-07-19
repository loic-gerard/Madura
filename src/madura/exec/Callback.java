/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package madura.exec;

import java.lang.reflect.Method;
import madura.debug.Log;

/**
 *
 * @author lgerard
 */
public class Callback {
	/***************************************************************************/
	/* ATTRIBUTS PRIVES*/
	
	
	/**
	 * Objet sur lequel est effectué le callback
	 */
	private Object target;
	
	/**
	 * Méthode appelée
	 */
	private String method;
	
	
	/***************************************************************************/
	/* CONSTRUCTEUR */
	
	/**
	 * Constructeur
	 * @param Object	in_target	Objet sur lequel est effectué le callback
	 * @param String	in_method	
	 */
	public Callback(Object in_target, String in_method){
		target = in_target;
		method = in_method;
	}
	
	
	/***************************************************************************/
	/* METHODES PUBLIQUES */
	
	/**
	 * Execute le callback
	 */
	public void call(){
		try {
			Class cl = target.getClass();
			String methodName = method;
			Method method = cl.getMethod(methodName);
			method.invoke(target); //
		} catch (Exception e) {
			Log.log("Erreur d'appel de la méthode "+method+"() sur l'objet "+target.getClass(), "madura.exec.Callback");
			e.printStackTrace();
		}
	}
	
	/**
	 * Execute le callback
	 * @param Object argument1 
	 */
	public void call(Object argument1){
		try {
			Class cl = target.getClass();
			String methodName = method;
			Method method = cl.getMethod(methodName, Object.class);
			method.invoke(target, argument1); //
		} catch (Exception e) {
			Log.log("Erreur d'appel de la méthode "+method+"(Object arg1) sur l'objet "+target.getClass(), "madura.exec.Callback");
			e.printStackTrace();
		}
	}
	
	/**
	 * Execute le callback
	 * @param argument1
	 * @param argument2 
	 */
	public void call(Object argument1, Object argument2){
		try {
			Class cl = target.getClass();
			String methodName = method;
			Method method = cl.getMethod(methodName, Object.class, Object.class);
			method.invoke(target, argument1, argument2); //
		} catch (Exception e) {
			Log.log("Erreur d'appel de la méthode "+method+"(Object arg1, Object arg2) sur l'objet "+target.getClass(), "madura.exec.Callback");
			e.printStackTrace();
		}
	}
}
