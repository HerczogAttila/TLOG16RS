/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.herczogattila.tlog16rs.core.exceptions;

/**
 *
 * @author precognox
 */
public class MissingUserException extends RuntimeException {
    public MissingUserException() {
        super("Missing user!");
    }
}
