/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.monitor.server.controllers;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;

import org.apache.metamodel.util.Action;
import org.apache.metamodel.util.FileHelper;
import org.datacleaner.job.AnalysisJob;
import org.datacleaner.job.AnalyzerJob;
import org.datacleaner.job.FilterJob;
import org.datacleaner.job.TransformerJob;
import org.datacleaner.monitor.configuration.TenantContext;
import org.datacleaner.monitor.configuration.TenantContextFactory;
import org.datacleaner.monitor.job.JobContext;
import org.datacleaner.monitor.scheduling.SchedulingService;
import org.datacleaner.monitor.scheduling.model.ScheduleDefinition;
import org.datacleaner.monitor.server.job.DataCleanerJobContext;
import org.datacleaner.monitor.shared.model.JobIdentifier;
import org.datacleaner.monitor.shared.model.SecurityRoles;
import org.datacleaner.monitor.shared.model.TenantIdentifier;
import org.datacleaner.repository.RepositoryFile;
import org.datacleaner.repository.RepositoryFolder;
import org.datacleaner.util.FileFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.net.UrlEscapers;

@Controller
@RequestMapping(value = "/{tenant}/jobs")
public class JobsFolderController {

    private static final Logger logger = LoggerFactory.getLogger(JobFileController.class);
    private static final String STATUS_SUCCESS = "Success";
    private static final String STATUS_FAILURE = "Failure";

    @Autowired
    TenantContextFactory _contextFactory;

    @Autowired
    SchedulingService _schedulingService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, String>> resultsFolderJson(@PathVariable("tenant") final String tenant) {
        final TenantContext context = _contextFactory.getContext(tenant);

        final List<Map<String, String>> result = new ArrayList<>();

        {
            final List<JobIdentifier> jobs = context.getJobs();
            for (final JobIdentifier job : jobs) {
                final JobContext jobContext = context.getJob(job);
                final RepositoryFile file = jobContext.getJobFile();

                final Map<String, String> map = new HashMap<>();
                map.put("name", job.getName());
                map.put("filename", file.getName());
                map.put("repository_path", file.getQualifiedPath());
                result.add(map);
            }
        }

        return result;
    }

    @RequestMapping(value = "/filter", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> getFolderJobsByMetadataProperty(@PathVariable("tenant") final String tenant,
            @RequestParam(value = "property", required = true) final String metadataProperty,
            @RequestParam(value = "value", required = true) final String metadataPropertyValue) {
        final TenantContext tenantContext = _contextFactory.getContext(tenant);
        final List<Map<String, Object>> result = new ArrayList<>();
        {
            final List<JobIdentifier> jobs = tenantContext.getJobs();
            for (final JobIdentifier job : jobs) {
                final JobContext jobContext = tenantContext.getJob(job);
                final Map<String, String> jobMetadataProperties = jobContext.getMetadataProperties();
                if (jobMetadataProperties.containsKey(metadataProperty)) {
                    final String metadataPropertyResult = jobMetadataProperties.get(metadataProperty);
                    if (metadataPropertyResult.toLowerCase().trim()
                            .equals(metadataPropertyValue.toLowerCase().trim())) {
                        final Map<String, Object> jobDetails = new HashMap<>();
                        final RepositoryFile file = jobContext.getJobFile();
                        jobDetails.put("name", job.getName());
                        jobDetails.put("filename", file.getName());
                        jobDetails.put("repository_path", file.getQualifiedPath());
                        jobDetails.put("metadataProperties", jobMetadataProperties);
                        final List<Map<String, Object>> descriptors = new ArrayList<>();

                        if (jobContext instanceof DataCleanerJobContext) {
                            final DataCleanerJobContext dcContext = (DataCleanerJobContext) jobContext;

                            final AnalysisJob analysisJob = dcContext.getAnalysisJob();
                            final List<AnalyzerJob> analyzerJobs = analysisJob.getAnalyzerJobs();
                            for (final AnalyzerJob analyzerJob : analyzerJobs) {
                                final Map<String, Object> jobComponent = new HashMap<>();
                                jobComponent.put("name", analyzerJob.getName());
                                jobComponent.put("type", "analyzer");
                                jobComponent.put("descriptor", analyzerJob.getDescriptor().getDisplayName());
                                jobComponent.put("metadataProperties", analyzerJob.getMetadataProperties());
                                descriptors.add(jobComponent);
                            }

                            final List<TransformerJob> transformerJobs = analysisJob.getTransformerJobs();
                            for (final TransformerJob transformerJob : transformerJobs) {
                                final Map<String, Object> jobComponent = new HashMap<>();
                                jobComponent.put("name", transformerJob.getName());
                                jobComponent.put("type", "transformer");
                                jobComponent.put("descriptor", transformerJob.getDescriptor().getDisplayName());
                                jobComponent.put("metadataProperties", transformerJob.getMetadataProperties());
                                descriptors.add(jobComponent);
                            }

                            final List<FilterJob> filterJobs = analysisJob.getFilterJobs();
                            for (final FilterJob filterJob : filterJobs) {
                                final Map<String, Object> jobComponent = new HashMap<>();
                                jobComponent.put("name", filterJob.getName());
                                jobComponent.put("type", "filter");
                                jobComponent.put("descriptor", filterJob.getDescriptor().getDisplayName());
                                jobComponent.put("metadataProperties", filterJob.getMetadataProperties());
                                descriptors.add(jobComponent);
                            }
                        }
                        jobDetails.put("descriptors", descriptors);
                        result.add(jobDetails);
                    }
                }
            }
        }
        return result;
    }

    @RolesAllowed(SecurityRoles.JOB_EDITOR)
    @RequestMapping(value = "/multiple_upload", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadMultipleAnalysisJobsToFolderHtml(@PathVariable("tenant") final String tenant,
            @RequestParam("files") final MultipartFile[] files) {
        final String[] fileNames = new String[files.length];
        String status = STATUS_SUCCESS;
        int i = 0;

        for (final MultipartFile singleFile : files) {
            final Map<String, String> outcome = uploadAnalysisJobToFolderJson(tenant, singleFile, null);
            fileNames[i] = UrlEscapers.urlFormParameterEscaper().escape(outcome.get("filename"));
            final boolean fileWasUploaded = isUploadedFilePresent(tenant, fileNames[i]);

            if (!fileWasUploaded) {
                status = STATUS_FAILURE;
                break;
            }

            if (!outcome.get("status").equals(STATUS_SUCCESS)) {
                status = outcome.get("status");
                break;
            }

            i++;
        }

        return getRedirectUrl(status, fileNames);
    }

    private String getRedirectUrl(final String status, final String[] fileNames) {
        final String baseUrl = "redirect:/scheduling";

        try {
            final String listOfFiles = URLEncoder.encode(String.join(", ", fileNames), "UTF-8");
            final String parameters = String.format("job_upload=%s&job_filename=%s", status, listOfFiles);
            return String.format("%s?%s", baseUrl, parameters);
        } catch (final UnsupportedEncodingException e) {
            return baseUrl;
        }
    }

    private boolean isUploadedFilePresent(final String tenant, final String fileName) {
        final TenantContext context = _contextFactory.getContext(tenant);
        final RepositoryFolder jobsFolder = context.getJobFolder();
        final RepositoryFile file = jobsFolder.getFile(fileName);

        return (file != null && file.getSize() > 0);
    }

    @RolesAllowed(SecurityRoles.JOB_EDITOR)
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadAnalysisJobToFolderHtml(@PathVariable("tenant") final String tenant,
            @RequestParam("file") final MultipartFile file,
            @RequestParam(value = "hotfolder", required = false) final String hotFolderLocation) {
        final Map<String, String> outcome = uploadAnalysisJobToFolderJson(tenant, file, hotFolderLocation);
        final String status = outcome.get("status");
        final String filename = UrlEscapers.urlFormParameterEscaper().escape(outcome.get("filename"));
        return "redirect:/scheduling?job_upload=" + status + "&job_filename=" + filename;
    }

    @RolesAllowed(SecurityRoles.JOB_EDITOR)
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public Map<String, String> uploadAnalysisJobToFolderJson(@PathVariable("tenant") final String tenant,
            @RequestParam("file") final MultipartFile file,
            @RequestParam(value = "hotfolder", required = false) final String hotFolderLocation) {
        if (file == null) {
            throw new IllegalArgumentException(
                    "No file upload provided. Please provide a multipart file using the 'file' HTTP parameter.");
        }

        final Action<OutputStream> writeCallback = out -> {
            final InputStream in = file.getInputStream();
            try {
                FileHelper.copy(in, out);
            } finally {
                FileHelper.safeClose(in);
            }
        };

        final TenantContext context = _contextFactory.getContext(tenant);
        final RepositoryFile jobFile;
        final RepositoryFolder jobsFolder = context.getJobFolder();

        final String filename = file.getOriginalFilename();

        jobFile = jobsFolder.createFile(filename, writeCallback);
        logger.info("Created new job from uploaded file: {}", filename);

        final TenantIdentifier tenantIdentifier = new TenantIdentifier(tenant);
        final ScheduleDefinition scheduleDefinition =
                _schedulingService.getSchedule(tenantIdentifier, new JobIdentifier(
                        filename.substring(0, filename.length() - FileFilters.ANALYSIS_XML.getExtension().length())));

        if (hotFolderLocation != null) {
            scheduleDefinition.setHotFolder(hotFolderLocation);
        }

        _schedulingService.updateSchedule(tenantIdentifier, scheduleDefinition);

        final Map<String, String> result = new HashMap<>();
        result.put("status", STATUS_SUCCESS);
        result.put("file_type", jobFile.getType().toString());
        result.put("filename", jobFile.getName());
        result.put("repository_path", jobFile.getQualifiedPath());

        return result;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    @ResponseBody
    public String handleInvalidInputException(final IllegalArgumentException exception) {
        return exception.getMessage();
    }

}
