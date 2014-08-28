package au.com.permeance.liferay.portlet.controller;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchContextFactory;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.SortFactoryUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.dynamicdatalists.model.DDLRecord;
import com.liferay.portlet.dynamicdatalists.model.DDLRecordConstants;
import com.liferay.portlet.dynamicdatalists.model.DDLRecordSet;
import com.liferay.portlet.dynamicdatalists.model.DDLRecordSetConstants;
import com.liferay.portlet.dynamicdatalists.service.DDLRecordLocalServiceUtil;
import com.liferay.portlet.dynamicdatalists.service.DDLRecordSetLocalServiceUtil;
import com.liferay.portlet.dynamicdatamapping.StructureDuplicateStructureKeyException;
import com.liferay.portlet.dynamicdatamapping.model.DDMStructure;
import com.liferay.portlet.dynamicdatamapping.model.DDMStructureConstants;
import com.liferay.portlet.dynamicdatamapping.service.DDMStructureLocalServiceUtil;
import com.liferay.portlet.dynamicdatamapping.storage.Fields;
import com.liferay.portlet.dynamicdatamapping.util.DDMIndexerUtil;

/**
 * Copyright (C) 2014 Permeance Technologies
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * @author tim.myerscough
 *
 */
@Controller("sampleportletController")
@RequestMapping("view")
public class SamplePortletController {

    private static final String PARAM_RECORDSET_ID = "recordsetId";
    private static Logger log = LoggerFactory.getLogger(SamplePortletController.class);

    private static final String TEMPLATE_NAME = "search-sort-test";
    private static final String RECORD_SET_NAME = TEMPLATE_NAME + "-recordset";

    @RenderMapping
    public String handleViewPage(final RenderRequest request, final ModelMap model,
            @RequestParam(defaultValue = "-1") final String recordSetIdParam, @RequestParam(required = false) final String columnName)
                    throws Exception {
        log.info("In View");

        long recordSetId = Long.parseLong(recordSetIdParam);
        DDLRecordSet recordSet = lookupTable(request);
        if (recordSetId == -1) {
            if (recordSet != null) {
                recordSetId = recordSet.getRecordSetId();
            }
        }

        if (recordSetId > 0) {

            List<DDLRecord> records = getRecords(request, recordSet, columnName);
            Table<Long, String, String> recordTable = HashBasedTable.create(records.size(), 7);

            long rowIdCounter = 0;
            for (DDLRecord record : records) {
                Fields fields = record.getFields();
                long rowId = rowIdCounter++ ;
                for (com.liferay.portlet.dynamicdatamapping.storage.Field field : fields) {
                    String name = field.getName();
                    String value = field.getValue(Locale.US).toString();

                    recordTable.put(rowId, name, value);

                }
            }
            model.addAttribute("recordTable", recordTable);
        }

        model.addAttribute("recordSetId", recordSetId);

        // Limit the number of rows to display.
        model.addAttribute("displayRows", Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));

        return "view";
    }

    private List<DDLRecord> getRecords(final RenderRequest request, final DDLRecordSet recordSet, final String sortByColumn)
            throws Exception {

        if (sortByColumn != null) {
            long startTime = System.currentTimeMillis();
            List<DDLRecord> sortedRecords = getSortedRecords(PortalUtil.getHttpServletRequest(request), recordSet, sortByColumn);
            long endTime = System.currentTimeMillis();

            log.info(String.format("Search Time: records= %d, time=%dms", sortedRecords.size(), endTime - startTime));
            return sortedRecords;
        } else {

            List<DDLRecord> records = recordSet.getRecords();
            return records;
        }

    }

    private List<DDLRecord> getSortedRecords(final HttpServletRequest request, final DDLRecordSet recordSet,
            final String sortByStructureColumnName) throws Exception {

        DDMStructure structure = recordSet.getDDMStructure();
        String indexedSortColumn = DDMIndexerUtil.encodeName(structure.getStructureId(), sortByStructureColumnName, Locale.US);

        SearchContext searchContext = SearchContextFactory.getInstance(request);
        String fieldType = structure.getFieldType(sortByStructureColumnName);
        int sortType = getSortType(fieldType);

        Sort sort = SortFactoryUtil.create(indexedSortColumn, sortType, false);

        searchContext.setSorts(sort);
        Indexer indexer = IndexerRegistryUtil.getIndexer("com.liferay.portlet.dynamicdatalists.util.DDLIndexer");
        Hits results = indexer.search(searchContext);

        return adapt(results);
    }

    private List<DDLRecord> adapt(final Hits results) throws Exception {

        List<DDLRecord> records = new LinkedList<DDLRecord>();
        for (Document document : results.getDocs()) {
            long recordId = GetterUtil.getLong(document.get(Field.ENTRY_CLASS_PK));

            DDLRecord ddlRecord = DDLRecordLocalServiceUtil.getDDLRecord(recordId);
            records.add(ddlRecord);
        }

        return records;
    }

    /**
     * Map the DDM Structure type to the sort field type
     */
    protected static int getSortType(final String sortField) {

        switch (sortField) {
            case "ddm-date":
                return Sort.LONG_TYPE;
            case "ddm-decimal":
            case "ddm-number":
                return Sort.DOUBLE_TYPE;
            default:
                return Sort.STRING_TYPE;
        }

    }

    private DDLRecordSet lookupTable(final PortletRequest request) throws Exception {
        ServiceContext context = ServiceContextFactory.getInstance(request);

        List<DDLRecordSet> recordSets = DDLRecordSetLocalServiceUtil.getRecordSets(context.getScopeGroupId());
        for (DDLRecordSet recordSet : recordSets) {
            if (recordSet.getRecordSetKey().equals(RECORD_SET_NAME)) {
                return recordSet;
            }
        }

        return null;
    }

    @ActionMapping("sort")
    public void sortRows(final ActionRequest actionRequest, final ActionResponse actionResponse, final ModelMap model,
            @RequestParam final String columnName) throws Exception {
        actionResponse.setRenderParameter("columnName", columnName);
    }

    @ActionMapping("add")
    public void addRows(final ActionRequest actionRequest, final ActionResponse actionResponse, final ModelMap model,
            @RequestParam final int rows) {

        try {
            DDLRecordSet recordSet = lookupTable(actionRequest);
            ServiceContext context = ServiceContextFactory.getInstance(actionRequest);

            for (int i = 0; i < rows; i++ ) {
                Map<String, Serializable> fieldsMap = new HashMap<String, Serializable>();

                fieldsMap.put("Boolean1637", RandomUtils.nextInt(0, 1) == 0);
                fieldsMap.put("Date1882", RandomUtils.nextLong(0, Long.MAX_VALUE));
                fieldsMap.put("Decimal2249", RandomUtils.nextDouble(0.0, 9999.0));

                fieldsMap.put("Number2495", RandomUtils.nextInt(0, 9999));

                String randomString = RandomStringUtils.randomAlphabetic(10);

                fieldsMap.put("Text2742", randomString);
                fieldsMap.put("Text_Box2985", randomString);
                fieldsMap.put("HTML3229", "<p>" + randomString + "</p>");

                DDLRecordLocalServiceUtil.addRecord(context.getUserId(), context.getScopeGroupId(), recordSet.getRecordSetId(),
                        DDLRecordConstants.DISPLAY_INDEX_DEFAULT, fieldsMap, context);
            }

            SessionMessages.add(actionRequest, "success");
        } catch (Exception e) {
            log.error("", e);
            SessionErrors.add(actionRequest, e.toString());
        }
    }

    @ActionMapping("create")
    public void createDdl(final ActionRequest actionRequest, final ActionResponse actionResponse, final ModelMap model) {
        log.info("In Action");
        try {

            long classNameId = PortalUtil.getClassNameId(DDMStructure.class);
            String structureKey = TEMPLATE_NAME;
            Map<Locale, String> nameMap = new HashMap<Locale, String>();
            nameMap.put(Locale.US, TEMPLATE_NAME);
            Map<Locale, String> descriptionMap = new HashMap<Locale, String>();
            descriptionMap.put(Locale.US, "Sample template");

            InputStream structureXsdStream = getClass().getResourceAsStream("/content/search-sort-test.xml");

            String xsd = IOUtils.toString(structureXsdStream);
            String storageType = "xml";
            int type = DDMStructureConstants.TYPE_DEFAULT;
            ServiceContext serviceContext = ServiceContextFactory.getInstance(actionRequest);
            long userId = serviceContext.getUserId();
            long groupId = serviceContext.getScopeGroupId();

            DDMStructure newStructure;

            try {
                newStructure = DDMStructureLocalServiceUtil.addStructure(userId, groupId, 0, classNameId, structureKey, nameMap,
                        descriptionMap, xsd, storageType, type, serviceContext);
            } catch (StructureDuplicateStructureKeyException e) {
                // already exists
                newStructure = DDMStructureLocalServiceUtil.getStructure(groupId, classNameId, structureKey);
            }

            int scope = DDLRecordSetConstants.SCOPE_DYNAMIC_DATA_LISTS;
            DDLRecordSet newRecordSet = DDLRecordSetLocalServiceUtil.addRecordSet(userId, groupId, newStructure.getStructureId(),
                    RECORD_SET_NAME, nameMap, descriptionMap, 0, scope, serviceContext);

            actionResponse.setRenderParameter(PARAM_RECORDSET_ID, "" + newRecordSet.getRecordSetId());

            SessionMessages.add(actionRequest, "success");
        } catch (Exception e) {
            log.error("", e);
            SessionErrors.add(actionRequest, e.toString());
        }
    }
}
