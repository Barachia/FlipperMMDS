/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ariaagent.managers;

import hmi.flipper.defaultInformationstate.DefaultRecord;

/**
 *
 * @author WaterschootJB
 */
public class InteractionManager extends DefaultManager{
    
    public InteractionManager(DefaultRecord is, long interval) {
        super(is, interval);
    }
    
}
