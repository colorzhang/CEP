CEP
===

Stream Computing and Complex Event Processing thoughts &amp; practices

Scenario
===
案例2：实时统计景区内游客人数
 
判断规则：
 
 * 1：最近10天内每天8:00-18:00在景区停留时长超过3小时天数小于5天
 
【每天8:00-18:00为一个计时周期，信令文件中第一天8:00后收到信令认为第一个本类
计时周期开始】
 
 * 2：最近10天内每天18:00到次日8:00在景区停留超过5小时小于5天
 
【每天18:00到次日8:00为一个计时周期，信令文件中第一天0:00-8:00间的信令可判定
为不属于任何计时周期，可忽略，即信令文件中第一天18:00后收到信令认为第一个本
类计时周期开始】
 
 * 3: 在网时长超过3个月
 
【所有用户均满足】


OEP code
===
~~~~~ xml
<?xml version="1.0" encoding="UTF-8"?>
<wlevs:config xmlns:wlevs="http://www.bea.com/ns/wlevs/config/application"
	xmlns:jdbc="http://www.oracle.com/ns/ocep/config/jdbc">
	<processor>
		<!--  
* 1：最近10天内每天8:00-18:00在景区停留时长超过3小时天数小于5天
 
【每天8:00-18:00为一个计时周期，信令文件中第一天8:00后收到信令认为第一个本类
计时周期开始】
 
 * 2：最近10天内每天18:00到次日8:00在景区停留超过5小时小于5天
 
【每天18:00到次日8:00为一个计时周期，信令文件中第一天0:00-8:00间的信令可判定
为不属于任何计时周期，可忽略，即信令文件中第一天18:00后收到信令认为第一个本
类计时周期开始】
		 -->
		<name>processor</name>
		<rules>
			<view id="tour3"> <![CDATA[ 
			SELECT
				tourists.imsi,
				tourists.signalingTime,
				tourists.lac,
				tourists.cell,
				tourists.mytime,
				tourists.durations
			FROM signalChannel 
        	MATCH_RECOGNIZE 
			(
			    PARTITION BY imsi
				MEASURES
					enter.imsi as imsi,
					enter.signalingTime as signalingTime,
					enter.lac as lac,
					enter.cell as cell,
					leave.mytime as mytime,
					(to_timestamp(leave.mytime, "yyyy-MM-dd HH:mm:ss.SSS") - to_timestamp(enter.mytime, "yyyy-MM-dd HH:mm:ss.SSS")) as durations
				PATTERN (enter intour* leave)
			 	DEFINE
			 		enter AS (enter.cell = "tourist") and (enter.mytime.substring(11) >= "08") and (enter.mytime.substring(11) <= "18"),
			 		intour AS (intour.cell = "tourist") and (intour.mytime.substring(11) >= "08") and (intour.mytime.substring(11) <= "18"),
			 		leave AS ((NOT (leave.cell = "tourist") or (leave.mytime.substring(11) > "18")) or (leave.mytime.substring(0,10) > enter.mytime.substring(0,10)))
			) AS tourists
			]]> </view>
			
			<view id="tour5"> <![CDATA[ 
			SELECT
				tourists.imsi,
				tourists.signalingTime,
				tourists.lac,
				tourists.cell,
				tourists.mytime,
				tourists.durations
			FROM signalChannel 
        	MATCH_RECOGNIZE 
			(
			    PARTITION BY imsi
				MEASURES
					enter.imsi as imsi,
					enter.signalingTime as signalingTime,
					enter.lac as lac,
					enter.cell as cell,
					leave.mytime as mytime,
					(to_timestamp(leave.mytime, "yyyy-MM-dd HH:mm:ss.SSS") - to_timestamp(enter.mytime, "yyyy-MM-dd HH:mm:ss.SSS")) as durations
				PATTERN (enter intour* leave)
			 	DEFINE
			 		enter AS (enter.cell = "tourist") and (enter.mytime.substring(11) >= "18"),
			 		intour AS ((intour.cell = "tourist") and ((intour.mytime.substring(11) <= "08") or (intour.mytime.substring(11) >= "18")) and (intour.mytime.substring(0,10) >= enter.mytime.substring(0,10))),
			 		leave AS ((NOT (leave.cell = "tourist")) or (leave.mytime.substring(11) > "08" and (leave.mytime.substring(0,10) > enter.mytime.substring(0,10))))
			) AS tourists
			]]> </view>
			
			<!-- 满足条件1 -->
			<view id="tourists1"> <![CDATA[ 
			SELECT imsi, count(*) as staydays
			FROM tour3 [RANGE 10 days]
			WHERE (durations >= INTERVAL "0 03:00:00.00" DAY TO SECOND)
			GROUP by imsi
			HAVING count(*) >= 5
			]]> </view>
			
			<!-- 满足条件2 -->
			<view id="tourists2"> <![CDATA[ 
			SELECT imsi, count(*) as staydays
			FROM tour5 [RANGE 10 days]
			WHERE (durations >= INTERVAL "0 05:00:00.00" DAY TO SECOND)
			GROUP by imsi
			HAVING count(*) >= 5
			]]> </view>
			
			<!-- 满足条件1 or 2 -->
			<query id="totaltourists"> <![CDATA[
			tourists1 UNION tourists2
			]]> </query>
		</rules>
	</processor>
</wlevs:config>

~~~~~

Esper code
===
~~~~~ sql
module winston.cep.esper;

import winston.cep.esper.*;
import java.sql.Timestamp;


create schema tour3 as (imsi string, signalingTime string, lac string, cell string, mytime string, durations long);

@Name('tour3')
insert into tour3
SELECT
  imsi,
	signalingTime,
	lac,
	cell,
	mytime,
	durations
FROM TourEvent 
MATCH_RECOGNIZE 
(
    PARTITION BY imsi
	MEASURES
		enter.imsi as imsi,
		enter.signalingTime as signalingTime,
		enter.lac as lac,
		enter.cell as cell,
		leave.mytime as mytime,
		Timestamp.valueOf(leave.mytime).getTime() - Timestamp.valueOf(enter.mytime).getTime() as durations
	PATTERN (enter intour* leave)
 	DEFINE
 		enter AS (enter.cell = 'tourist') and (enter.mytime.substring(11) >= '08') and (enter.mytime.substring(11) <= '18'),
		intour AS (intour.cell = 'tourist') and (intour.mytime.substring(11) >= '08') and (intour.mytime.substring(11) <= '18'),
		leave AS ((NOT (leave.cell = 'tourist') or (leave.mytime.substring(11) > '18')) or (leave.mytime.substring(0,10) > enter.mytime.substring(0,10)))
);


create schema tour5 as (imsi string, signalingTime string, lac string, cell string, mytime string, durations long);

@Name('tour5')
insert into tour5
SELECT
	imsi,
	signalingTime,
	lac,
	cell,
	mytime,
	durations
FROM TourEvent 
MATCH_RECOGNIZE 
(
    PARTITION BY imsi
	MEASURES
		enter.imsi as imsi,
		enter.signalingTime as signalingTime,
		enter.lac as lac,
		enter.cell as cell,
		leave.mytime as mytime,
		Timestamp.valueOf(leave.mytime).getTime() - Timestamp.valueOf(enter.mytime).getTime() as durations
	PATTERN (enter intour* leave)
 	DEFINE
 		enter AS (enter.cell = "tourist") and (enter.mytime.substring(11) >= "18"),
 		intour AS ((intour.cell = "tourist") and ((intour.mytime.substring(11) <= "08") or (intour.mytime.substring(11) >= "18")) and (intour.mytime.substring(0,10) >= enter.mytime.substring(0,10))),
 		leave AS ((NOT (leave.cell = "tourist")) or (leave.mytime.substring(11) > "08" and (leave.mytime.substring(0,10) > enter.mytime.substring(0,10))))
);


create schema tourists1 as (imsi string, staydays long);

@Name('tourists1')
insert into tourists1
SELECT imsi, count(*) as staydays
FROM tour3.win:time(10 days)
WHERE durations >= 3*3600*1000
GROUP by imsi
HAVING count(*) >= 5;

create schema tourists2 as (imsi string, staydays long);

@Name('tourists2')
insert into tourists2
SELECT imsi, count(*) as staydays
FROM tour5.win:time(10 days)
WHERE durations >= 5*3600*1000
GROUP by imsi
HAVING count(*) >= 5;


create schema totaltourists as (imsi string, staydays long);

@Name('totaltourists')
insert into totaltourists
select distinct t1.imsi as imsi, t1.staydays as staydays from tourists1.win:time(10 days) as t1, tourists2.win:time(10 days) as t2;


@Name('totaltouristsnodup')
select * from totaltourists.std:firstunique(imsi);

~~~~~

colorzhang@gmail.com
