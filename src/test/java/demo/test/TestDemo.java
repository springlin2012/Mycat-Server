/*
 * Copyright (c) 2016 xiaoniu, Inc. All rights reserved.
 *
 * @author chunlin.li https://github.com/springlin2012
 *
 */
package demo.test;

import java.sql.SQLSyntaxErrorException;

/**
 * 功能描述:
 * <p/>
 * 创建人: chunlin.li
 * <p/>
 * 创建时间: 2016/08/01.
 * <p/>
 */
public class TestDemo {


    public static void main(String[] args) {

        int num = 10;
        printInfo(num);
        num = num << 5;
        /*printInfo(num);
        System.out.println(num);*/

        String origSQL = "INSERT INTO sam_test(id, name, age) VALUES (1, '21313', '18')";
        int firstLeftBracketIndex = origSQL.indexOf("(");
        int valuesIndex = origSQL.indexOf("VALUES");

        if(valuesIndex + "VALUES".length() <= firstLeftBracketIndex) {
            System.out.println("insert must provide ColumnList");
        }

    }


    /**
     26      * 输出一个int的二进制数
     27      * @param num
     28      */
    private static void printInfo(int num){
        System.out.println(Integer.toBinaryString(num));
    }





}
