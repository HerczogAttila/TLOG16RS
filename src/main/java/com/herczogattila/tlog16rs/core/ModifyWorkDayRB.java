/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.herczogattila.tlog16rs.core;

/**
 *
 * @author precognox
 */
@lombok.Getter
@lombok.Setter
public class ModifyWorkDayRB {
    private int year;
    private int month;
    private int day;
    private int requiredMinutes;
}
