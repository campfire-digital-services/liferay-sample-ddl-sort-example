
# This repo is archived. Please note that Liferay 6.2 CE, being before the current 7.4, is almost certainly beyond both the Premium and Limited Support phases and so is end of life and should be upgraded or decommissioned as soon as possible
# https://www.liferay.com/en-AU/subscription-services/end-of-life/liferay-dxp



liferay-sample-ddl-sort-example
===============================

Sample Liferay portlet to demonstrate searching/sorting with DDLs

https://www.permeance.com.au/web/tim.myerscough/home/-/blogs/sorting-liferay-ddl-entries

Pre-Requisites
==============
To build this portlet, you need [Apache Maven](http://maven.apache.org/) and JDK 1.7+

Setup
=====
* [Install Liferay 6.2 CE](https://www.liferay.com/downloads/liferay-portal/available-releases)

Installation
============
Checkout the project and build it using

    mvn clean install
    
Copy the built WAR file to your Liferay deploy folder.

Use
===
* Add this portlet to a page (Sample > sample)
* Click the link to create the DDM Structure
* Add some rows to the table
* Select a column heading and paste into the corresponding input field and hit seearch to see the power of sorting DDL lists!
