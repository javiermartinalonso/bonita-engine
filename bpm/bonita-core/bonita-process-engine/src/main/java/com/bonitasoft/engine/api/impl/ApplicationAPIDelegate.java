/*******************************************************************************
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.api.impl;

import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.exceptions.SObjectAlreadyExistsException;
import org.bonitasoft.engine.commons.exceptions.SObjectCreationException;
import org.bonitasoft.engine.commons.exceptions.SObjectNotFoundException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.RetrieveException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.search.SearchResult;

import com.bonitasoft.engine.api.impl.convertor.ApplicationConvertor;
import com.bonitasoft.engine.api.impl.transaction.application.SearchApplications;
import com.bonitasoft.engine.business.application.Application;
import com.bonitasoft.engine.business.application.ApplicationCreator;
import com.bonitasoft.engine.business.application.ApplicationNotFoundException;
import com.bonitasoft.engine.business.application.ApplicationPage;
import com.bonitasoft.engine.business.application.ApplicationPageNotFoundException;
import com.bonitasoft.engine.business.application.ApplicationService;
import com.bonitasoft.engine.business.application.SApplication;
import com.bonitasoft.engine.business.application.SApplicationPage;
import com.bonitasoft.engine.business.application.SApplicationPageBuilder;
import com.bonitasoft.engine.business.application.SApplicationPageBuilderFactory;
import com.bonitasoft.engine.service.TenantServiceAccessor;

/**
 * @author Elias Ricken de Medeiros
 *
 */
public class ApplicationAPIDelegate {

    private final ApplicationConvertor convertor;
    private final long loggedUserId;
    private final SearchApplications searchApplications;
    private final ApplicationService applicationService;

    public ApplicationAPIDelegate(final TenantServiceAccessor accessor, final ApplicationConvertor convertor, final long loggedUserId,
            final SearchApplications searchApplications) {
        applicationService = accessor.getApplicationService();
        this.convertor = convertor;
        this.loggedUserId = loggedUserId;
        this.searchApplications = searchApplications;
    }

    public Application createApplication(final ApplicationCreator applicationCreator) throws AlreadyExistsException, CreationException {
        try {
            final SApplication sApplication = applicationService.createApplication(convertor.buildSApplication(applicationCreator, loggedUserId));
            return convertor.toApplication(sApplication);
        } catch (final SObjectCreationException e) {
            throw new CreationException(e);
        } catch (final SObjectAlreadyExistsException e) {
            throw new AlreadyExistsException(e.getMessage());
        }
    }

    public Application getApplication(final long applicationId) throws ApplicationNotFoundException {
        try {
            final SApplication sApplication = applicationService.getApplication(applicationId);
            return convertor.toApplication(sApplication);
        } catch (final SBonitaReadException e) {
            throw new RetrieveException(e);
        } catch (final SObjectNotFoundException e) {
            throw new ApplicationNotFoundException(applicationId);
        }
    }

    public void deleteApplication(final long applicationId) throws DeletionException {
        try {
            applicationService.deleteApplication(applicationId);
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        }
    }

    public SearchResult<Application> searchApplications() throws SearchException {
        try {
            searchApplications.execute();
            return searchApplications.getResult();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
    }

    public ApplicationPage createApplicationPage(final long applicationId, final long pagedId, final String name) throws AlreadyExistsException,    CreationException {
        final SApplicationPageBuilderFactory factory = BuilderFactory.get(SApplicationPageBuilderFactory.class);
        final SApplicationPageBuilder builder = factory.createNewInstance(applicationId, pagedId, name);
        SApplicationPage sAppPage;
        try {
            sAppPage = applicationService.createApplicationPage(builder.done());
            return convertor.toApplicationPage(sAppPage);
        } catch (final SObjectCreationException e) {
            throw new CreationException(e);
        } catch (final SObjectAlreadyExistsException e) {
            throw new AlreadyExistsException(e.getMessage());
        }
    }

    public ApplicationPage getApplicationPage(final String applicationName, final String applicationPageName) throws ApplicationPageNotFoundException {
        try {
            final SApplicationPage sAppPage = applicationService.getApplicationPage(applicationName, applicationPageName);
            return convertor.toApplicationPage(sAppPage);
        } catch (final SBonitaReadException e) {
            throw new RetrieveException(e);
        } catch (final SObjectNotFoundException e) {
            throw new ApplicationPageNotFoundException(e.getMessage());
        }
    }

    public ApplicationPage getApplicationPage(final long applicationPageId) throws ApplicationPageNotFoundException {
        try {
            final SApplicationPage sApplicationPage = applicationService.getApplicationPage(applicationPageId);
            return convertor.toApplicationPage(sApplicationPage);
        } catch (final SBonitaReadException e) {
            throw new RetrieveException(e);
        } catch (final SObjectNotFoundException e) {
            throw new ApplicationPageNotFoundException(e.getMessage());
        }
    }

    public void deleteApplicationPage(final long applicationpPageId) throws DeletionException {
        try {
            applicationService.deleteApplicationPage(applicationpPageId);
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        }
    }

}
