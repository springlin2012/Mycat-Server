/*
 * Copyright (c) 2013, OpenCloudDB/MyCAT and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software;Designed and Developed mainly by many Chinese 
 * opensource volunteers. you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License version 2 only, as published by the
 * Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Any questions about this component can be directed to it's project Web address 
 * https://code.google.com/p/opencloudb/.
 *
 */
package org.opencloudb.route.function;

import org.junit.Assert;
import org.junit.Test;

public class PartitionByPatternTest {

    /**
     *
     *   ###### first host configuration
         1-32=0
         33-64=1
         65-96=2
         97-128=3
         ######## second host configuration
         129-160=4
         161-192=5
         193-224=6
         225-256=7
         0-0=7
     *
     */

	@Test
	public void test() {
		PartitionByPattern autoPartition = new PartitionByPattern();
		autoPartition.setPatternValue(256);
		autoPartition.setDefaultNode(2);
		autoPartition.setMapFile("partition-pattern.txt");
		autoPartition.init();

		String idVal = "0";
        System.out.println(autoPartition.calculate(idVal));
        Assert.assertEquals(true, 7 == autoPartition.calculate(idVal));

		idVal = "45254875490";
        System.out.println(Long.valueOf(idVal)%256);
        System.out.println(autoPartition.calculate(idVal));
		//Assert.assertEquals(true, 2 == autoPartition.calculate(idVal));

	}
}