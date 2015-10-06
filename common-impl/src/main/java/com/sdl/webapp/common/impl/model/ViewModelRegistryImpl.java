package com.sdl.webapp.common.impl.model;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sdl.webapp.common.api.mapping.SemanticMapping;
import com.sdl.webapp.common.api.mapping.annotations.SemanticEntities;
import com.sdl.webapp.common.api.mapping.annotations.SemanticEntity;
import com.sdl.webapp.common.api.mapping.annotations.SemanticEntityInfo;
import com.sdl.webapp.common.api.model.MvcData;
import com.sdl.webapp.common.api.model.ViewModel;
import com.sdl.webapp.common.api.model.ViewModelRegistry;
import com.sdl.webapp.common.api.model.entity.*;
import com.sdl.webapp.common.api.model.page.AbstractPageModelImpl;
import com.sdl.webapp.common.api.model.page.PageModelImpl;
import com.sdl.webapp.common.api.model.region.RegionModelImpl;
import com.sdl.webapp.common.api.model.region.SimpleRegionMvcData;
import com.sdl.webapp.common.exceptions.DxaException;
import com.sdl.webapp.common.impl.mapping.MvcDataImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of {@code ViewModelRegistry}.
 */
@Component
public class ViewModelRegistryImpl implements ViewModelRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ViewModelRegistryImpl.class);

    private final Map<MvcData, Class<? extends ViewModel>> viewEntityClassMap = new HashMap<>();
    private final Map<Class<? extends ViewModel>, SemanticInfo> modelTypeToSemanticInfoMapping = new HashMap<>();
    private final Map<String, List<Class<? extends ViewModel>>> semanticTypeToModelTypesMapping = new HashMap<>();

    //TODO: Check whether this is really autowired
    @Autowired
    private SemanticMapping semanticMapping;
    private Lock lock;

    //TODO : initialize these in the core module
    public ViewModelRegistryImpl() {
        this.lock = new ReentrantLock();
        try {
            this.registerViewEntityClass("Article", Article.class);
            this.registerViewEntityClass("Breadcrumb", NavigationLinks.class);
            this.registerViewEntityClass("Carousel", ItemList.class);
            this.registerViewEntityClass("CookieNotificationBar", Notification.class);
            this.registerViewEntityClass("Download", Download.class);
            this.registerViewEntityClass("FooterLinkGroup", LinkList.class);
            this.registerViewEntityClass("FooterLinks", LinkList.class);
            this.registerViewEntityClass("HeaderLinks", LinkList.class);
            this.registerViewEntityClass("HeaderLogo", Teaser.class);
            this.registerViewEntityClass("Image", Image.class);
            this.registerViewEntityClass("LeftNavigation", NavigationLinks.class);
            this.registerViewEntityClass("LanguageSelector", Configuration.class);
            this.registerViewEntityClass("List", ContentList.class);
            this.registerViewEntityClass("OldBrowserNotificationBar", Notification.class);
            this.registerViewEntityClass("PagedList", ContentList.class);
            this.registerViewEntityClass("Place", Place.class);
            this.registerViewEntityClass("SiteMap", SitemapItem.class);
            this.registerViewEntityClass("SiteMapXml", SitemapItem.class);
            this.registerViewEntityClass("SocialLinks", TagLinkList.class);
            this.registerViewEntityClass("SocialSharing", TagLinkList.class);
            this.registerViewEntityClass("Tab", ItemList.class);
            this.registerViewEntityClass("Teaser-ImageOverlay", Teaser.class);
            this.registerViewEntityClass("Teaser", Teaser.class);
            this.registerViewEntityClass("TeaserColored", Teaser.class);
            this.registerViewEntityClass("TeaserHero-ImageOverlay", Teaser.class);
            this.registerViewEntityClass("TeaserMap", Teaser.class);
            this.registerViewEntityClass("ThumbnailList", ContentList.class);
            this.registerViewEntityClass("TopNavigation", NavigationLinks.class);
            this.registerViewEntityClass("YouTubeVideo", YouTubeVideo.class);

            this.registerPageViewModel("GeneralPage", PageModelImpl.class);
            this.registerPageViewModel("IncludePage", PageModelImpl.class);
            this.registerPageViewModel("RedirectPage", PageModelImpl.class);

            this.registerRegionViewModel("2-Column", RegionModelImpl.class);
            this.registerRegionViewModel("3-Column", RegionModelImpl.class);
            this.registerRegionViewModel("4-Column", RegionModelImpl.class);
            this.registerRegionViewModel("Hero", RegionModelImpl.class);
            this.registerRegionViewModel("Info", RegionModelImpl.class);
            this.registerRegionViewModel("Left", RegionModelImpl.class);
            this.registerRegionViewModel("Links", RegionModelImpl.class);
            this.registerRegionViewModel("Logo", RegionModelImpl.class);
            this.registerRegionViewModel("Main", RegionModelImpl.class);
            this.registerRegionViewModel("Nav", RegionModelImpl.class);
            this.registerRegionViewModel("Tools", RegionModelImpl.class);


            this.registerRegionViewModel("Header", RegionModelImpl.class);
            this.registerRegionViewModel("Footer", RegionModelImpl.class);
            this.registerRegionViewModel("Left Navigation", RegionModelImpl.class);
            this.registerRegionViewModel("Content Tools", RegionModelImpl.class);

        } catch (DxaException e) {
            e.printStackTrace();
        }
    }

    private static final String DEFAULT_AREA_NAME = "Core";
    private static final String PAGE_CONTROLLER_NAME = "Page";
    private static final String REGION_CONTROLLER_NAME = "Region";
    private static final String PAGE_ACTION_NAME = "Page";
    private static final String REGION_ACTION_NAME = "Region";

    @Override
    public void registerPageViewModel(String viewName, Class<? extends ViewModel> pageModelClass) throws DxaException {
        MvcDataImpl mvcData = new MvcDataImpl(viewName);
        mvcData.setControllerAreaName(DEFAULT_AREA_NAME);
        mvcData.setControllerName(PAGE_CONTROLLER_NAME);
        mvcData.setActionName(PAGE_ACTION_NAME);
        registerViewModel(mvcData, pageModelClass);
    }
    @Override
    public void registerRegionViewModel(String viewName, Class<? extends ViewModel> regionModelClass) throws DxaException {
        MvcDataImpl mvcData = new MvcDataImpl(viewName);
        mvcData.setControllerAreaName(DEFAULT_AREA_NAME);
        mvcData.setControllerName(REGION_CONTROLLER_NAME);
        mvcData.setActionName(REGION_ACTION_NAME);
        registerViewModel(mvcData, regionModelClass);
    }
    private class SemanticInfo {
        final Map<String, String> prefixMappings = new HashMap<>();
        final List<String> publicSemanticTypes = new ArrayList<String>();
        final List<String> mappedSemanticTypes = new ArrayList<String>();
        final Map<String, List<String>> semanticProperties = new HashMap<>();
    }

    @Override
    public void registerViewModel(MvcData viewData, Class<? extends ViewModel> entityClass) {
        try {
            if (lock.tryLock(10, TimeUnit.SECONDS)) {
                if (viewData != null) {
                    if (this.viewEntityClassMap.containsKey(viewData)) {
                        LOG.warn("View '%s' registered multiple times.", viewData);
                        return;
                    }
                    viewEntityClassMap.put(viewData, entityClass);
                }

                if (!modelTypeToSemanticInfoMapping.containsKey(entityClass)) {
                    registerModelType(entityClass);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (DxaException e) {
            e.printStackTrace();
        } finally {
            //release lock
            lock.unlock();
        }
    }

    private SemanticInfo registerModelType(Class<? extends ViewModel> modelType) throws DxaException {
        SemanticInfo semanticInfo = extractSemanticInfo(modelType);
        modelTypeToSemanticInfoMapping.put(modelType, semanticInfo);

        for (String semanticTypeName : semanticInfo.mappedSemanticTypes) {
            List<Class<? extends ViewModel>> mappedModelTypes;
            if (!semanticTypeToModelTypesMapping.containsKey(semanticTypeName)) {
                mappedModelTypes = new ArrayList<Class<? extends ViewModel>>();
                this.semanticTypeToModelTypesMapping.put(semanticTypeName, mappedModelTypes);
            } else {
                mappedModelTypes = semanticTypeToModelTypesMapping.get(semanticTypeName);
            }
            mappedModelTypes.add(modelType);
        }

        if (!semanticInfo.publicSemanticTypes.isEmpty()) {
            LOG.debug("Model type '%s' has semantic type(s) '%s'.", modelType.getName(), semanticInfo.publicSemanticTypes); //StringUtils.join(semanticInfo.PublicSemanticTypes, " "));
            for (Map.Entry<String, List<String>> kvp : semanticInfo.semanticProperties.entrySet()) {
                LOG.debug("\tRegistered property '%s' as semantic property '%s'", kvp.getKey(), kvp.getValue()); //StringUtils.join(kvp.getValue(), " "));
            }
        }
        return semanticInfo;
    }

    private SemanticInfo extractSemanticInfoFromAnnotation(SemanticEntity attribute, SemanticInfo si) throws DxaException {
        SemanticInfo semanticInfo = si;
        if(si==null){
            semanticInfo = new SemanticInfo();
        }
        semanticInfo.mappedSemanticTypes.add(semanticMapping.getQualifiedTypeName(attribute.entityName(), attribute.vocabulary()));

        if (!attribute.public_() || attribute.prefix()==null || attribute.prefix().trim().equals(""))
            return semanticInfo;

        String prefix = attribute.prefix();
        String registeredVocab;
        if (semanticInfo.prefixMappings.containsKey(prefix))
        {
            registeredVocab = semanticInfo.prefixMappings.get(prefix);
            // Prefix mapping already exists; must match.
            if (attribute.vocabulary() != registeredVocab)
            {
                throw new DxaException(
                        String.format("Attempt to use semantic prefix '{}' for vocabulary '{}', but is is already used for vocabulary '{3}",
                                prefix, attribute.vocabulary(), registeredVocab)
                );
            }
        }
        else
        {
            semanticInfo.prefixMappings.put(prefix, attribute.vocabulary());
        }

        semanticInfo.publicSemanticTypes.add(String.format("{}:{}", prefix, attribute.entityName()));
        return semanticInfo;
    }

    private SemanticInfo extractSemanticInfo(Class<? extends ViewModel> modelType) throws DxaException {


        SemanticInfo semanticInfo = new SemanticInfo();

        // Built-in semantic type mapping
        String bareTypeName = modelType.getSimpleName();
        semanticInfo.mappedSemanticTypes.add(semanticMapping.getQualifiedTypeName(bareTypeName, null));

        // Extract semantic info from SemanticEntity attributes on the Model Type.
        if(modelType.isAnnotationPresent(SemanticEntity.class)){
            semanticInfo = extractSemanticInfoFromAnnotation(modelType.getAnnotation(SemanticEntity.class), null);
        }else if(modelType.getClass().isAnnotationPresent(SemanticEntities.class)){
            SemanticEntities annotations = modelType.getClass().getAnnotation(SemanticEntities.class);
            for (SemanticEntity attribute : annotations.value())
            {
                    semanticInfo = extractSemanticInfoFromAnnotation(attribute, semanticInfo);
            }
        }


        // Extract semantic info from SemanticEntity attributes on the Model Type's properties
//        foreach (MemberInfo memberInfo in modelType.GetMembers(BindingFlags.Public | BindingFlags.Instance))
//        {
//            foreach (SemanticPropertyAttribute attribute in memberInfo.GetCustomAttributes(true).Where(a => a is SemanticPropertyAttribute))
//            {
//                if (string.IsNullOrEmpty(attribute.PropertyName))
//                {
//                    // Skip properties without name.
//                    continue;
//                }
//                string[] semanticPropertyNameParts = attribute.PropertyName.Split(':');
//                if (semanticPropertyNameParts.Length < 2)
//                {
//                    // Skip property names without prefix.
//                    continue;
//                }
//                string prefix = semanticPropertyNameParts[0];
//                if (!semanticInfo.PrefixMappings.ContainsKey(prefix))
//                {
//                    // Skip property names with prefix which is not declared as public prefix on the type.
//                    continue;
//                }
//
//                IList<string> semanticPropertyNames;
//                if (!semanticInfo.SemanticProperties.TryGetValue(memberInfo.Name, out semanticPropertyNames))
//                {
//                    semanticPropertyNames = new List<string>();
//                    semanticInfo.SemanticProperties.Add(memberInfo.Name, semanticPropertyNames);
//                }
//                semanticPropertyNames.Add(attribute.PropertyName);
//            }
//        }


        return semanticInfo;
    }

    @Override
    public Class<? extends ViewModel> getViewModelType(final MvcData viewData) throws DxaException {
        Class modelType = null;

        Predicate<Map.Entry<MvcData, Class<? extends ViewModel>>> keyNamePredicate =
                new Predicate<Map.Entry<MvcData, Class<? extends ViewModel>>>() {
                    @Override
                    public boolean apply(Map.Entry<MvcData, Class<? extends ViewModel>> input) {
                        MvcData thisKey = input.getKey();
                        return thisKey.getViewName().equals(viewData.getViewName()) &&
                                ((thisKey.getControllerAreaName() == null && viewData.getControllerAreaName() == null ) ||(thisKey.getControllerAreaName().equals(viewData.getControllerAreaName()))) &&
                                ((thisKey.getControllerName() == null && viewData.getControllerName() == null ) ||(thisKey.getControllerName().equals(viewData.getControllerName()))) &&
                                thisKey.getAreaName().equals(viewData.getAreaName());
                    }
                };
        Predicate<Map.Entry<MvcData, Class<? extends ViewModel>>> keyNamePredicateNoArea =
                new Predicate<Map.Entry<MvcData, Class<? extends ViewModel>>>() {
                    @Override
                    public boolean apply(Map.Entry<MvcData, Class<? extends ViewModel>> input) {
                        MvcData thisKey = input.getKey();
                        return thisKey.getViewName().equals(viewData.getViewName()) &&
                                ((thisKey.getControllerAreaName() == null && viewData.getControllerAreaName() == null ) ||(thisKey.getControllerAreaName().equals(viewData.getControllerAreaName()))) &&
                                ((thisKey.getControllerName() == null && viewData.getControllerName() == null ) ||(thisKey.getControllerName().equals(viewData.getControllerName())));
                    }
                };
        Map<MvcData, Class<? extends ViewModel>> possibleValues = Maps.filterEntries(this.viewEntityClassMap, keyNamePredicate);
        if (possibleValues.isEmpty()) {
            //first let's see if there is another relevant view
            possibleValues = Maps.filterEntries(this.viewEntityClassMap, keyNamePredicateNoArea);
        }
        if (possibleValues.isEmpty()) {
            throw new DxaException(String.format("Could not find a view model for the view data %s", viewData));
        } else {
            return possibleValues.entrySet().iterator().next().getValue();
        }
    }

    @Override
    public Class<? extends ViewModel> getMappedModelTypes(String semanticTypeName) throws DxaException {
        MvcData mvcData = new MvcDataImpl(semanticTypeName);
        return getViewModelType(mvcData);
        //TODO : implement this correctly, based on semantics
    }

    @Override
    public Class<? extends ViewModel> getViewEntityClass(final String viewName) throws DxaException {

        final String areaName;
        final String scopedViewName;
        if ( !viewName.contains(":") ) { // Core module
            areaName = "Core";
            scopedViewName = viewName;
        }
        else {
            String[] parts = viewName.split(":");
            areaName = parts[0];
            scopedViewName = parts[1];
        }
        Predicate<Map.Entry<MvcData, Class<? extends ViewModel>>> keyNamePredicate =
                new Predicate<Map.Entry<MvcData, Class<? extends ViewModel>>>() {
                    @Override
                    public boolean apply(Map.Entry<MvcData, Class<? extends ViewModel>> input) {
                        return input.getKey().getAreaName().equals(areaName) && input.getKey().getViewName().equals(scopedViewName);
                    }
                };


        Map<MvcData, Class<? extends ViewModel>> possibleValues = Maps.filterEntries(this.viewEntityClassMap, keyNamePredicate);
        if (possibleValues.isEmpty()) {
            throw new DxaException(String.format("Could not find a view model for the view name %s", viewName));
        } else {
            return possibleValues.entrySet().iterator().next().getValue();
        }

    }


    @Override
    public void registerViewEntityClass(String viewName, Class<? extends ViewModel> entityClass) throws DxaException {
        MvcData mvcData = new MvcDataImpl(viewName);
        registerViewModel(mvcData, entityClass);
    }


  /*
        /// <summary>
        /// Registers a View Model mapping by compiling a given view file and obtaining its model type.
        /// </summary>
        /// <param name="viewData">The data for the View to register.</param>
        /// <param name="viewVirtualPath">The (virtual) path to the View file.</param>
        public static void RegisterViewModel(MvcData viewData, string viewVirtualPath)
        {
            using (new Tracer(viewData, viewVirtualPath))
            {
                lock (_viewToModelTypeMapping)
                {
                    if (_viewToModelTypeMapping.ContainsKey(viewData))
                    {
                        Log.Warn("View '{0}' registered multiple times. Virtual Path: '{1}'", viewData, viewVirtualPath);
                        return;
                    }

                    try
                    {
                        Type compiledViewType = BuildManager.GetCompiledType(viewVirtualPath);
                        if (!compiledViewType.BaseType.IsGenericType)
                        {
                            throw new DxaException("View is not strongly typed. Please ensure you use the @model directive.");
                        }
                        RegisterViewModel(viewData, compiledViewType.BaseType.GetGenericArguments()[0]);
                    }
                    catch (Exception ex)
                    {
                        throw new DxaException(string.Format("Error occurred while compiling View '{0}'", viewVirtualPath), ex);
                    }
                }
            }
        }

        /// <summary>
        /// Get the View Model Type for a given View.
        /// </summary>
        /// <param name="viewData">The data for the View.</param>
        /// <returns>The View Model Type.</returns>
        public static Type GetViewModelType(MvcData viewData)
        {
            Type modelType;
            MvcData bareMvcData = new MvcData
            {
                AreaName = viewData.AreaName,
                ControllerName = viewData.ControllerName,
                ViewName = viewData.ViewName
            };
            if (!_viewToModelTypeMapping.TryGetValue(bareMvcData, out modelType))
            {
                throw new DxaException(
                    string.Format("No View Model registered for View '{0}'. Check that you have registered this View in the '{1}' area registration.", viewData, viewData.AreaName)
                    );
            }
            return modelType;
        }

        /// <summary>
        /// Gets the semantic types (and prefix mappings) for a given Model Type.
        /// </summary>
        /// <param name="modelType">The Model Type.</param>
        /// <param name="prefixMappings">The prefix mappings for the prefixes used by the types.</param>
        /// <returns>The semantic types.</returns>
        public static string[] GetSemanticTypes(Type modelType, out IDictionary<string, string> prefixMappings)
        {
            // No Tracer here to reduce trace noise.
            SemanticInfo semanticInfo = GetSemanticInfo(modelType);
            prefixMappings = semanticInfo.PrefixMappings;
            return semanticInfo.PublicSemanticTypes.ToArray();
        }
*/
        /// <summary>
        /// Gets the semantic property names for a given Model Type and property name.
        /// </summary>
        /// <param name="modelType">The Model Type.</param>
        /// <param name="propertyName">The property name.</param>
        /// <returns>The semantic property names or <c>null</c> if no semantic property names have been registered for the given property.</returns>

        public  String[] getSemanticPropertyNames(Class<? extends ViewModel> modelType, String propertyName) throws DxaException {
            // No Tracer here to reduce trace noise.
            SemanticInfo semanticInfo = getSemanticInfo(modelType);

            List<String> semanticPropertyNames;
//            if (semanticInfo.semanticProperties().TryGetValue(propertyName, out semanticPropertyNames))
//            {
//                return semanticPropertyNames.ToArray();
//            }
            return null;
        }
/*
        /// <summary>
        /// Gets the Model Types mapped to a given semantic type name.
        /// </summary>
        /// <param name="semanticTypeName">The semantic type name qualified with vocabulary ID.</param>
        /// <returns>The mapped model types or <c>null</c> if no Model types are registered for the given semantic type name.</returns>
        public static IEnumerable<Type> GetMappedModelTypes(string semanticTypeName)
        {
            ISet<Type> mappedModelTypes;
            _semanticTypeToModelTypesMapping.TryGetValue(semanticTypeName, out mappedModelTypes);
            return mappedModelTypes;
        }

        private static SemanticInfo RegisterModelType(Type modelType)
        {
            using (new Tracer(modelType))
            {
                SemanticInfo semanticInfo = ExtractSemanticInfo(modelType);
                _modelTypeToSemanticInfoMapping.Add(modelType, semanticInfo);

                foreach (string semanticTypeName in semanticInfo.MappedSemanticTypes)
                {
                    ISet<Type> mappedModelTypes;
                    if (!_semanticTypeToModelTypesMapping.TryGetValue(semanticTypeName, out mappedModelTypes))
                    {
                        mappedModelTypes = new HashSet<Type>();
                        _semanticTypeToModelTypesMapping.Add(semanticTypeName, mappedModelTypes);
                    }
                    mappedModelTypes.Add(modelType);
                }

                if (semanticInfo.PublicSemanticTypes.Any())
                {
                    Log.Debug("Model type '{0}' has semantic type(s) '{1}'.", modelType.FullName, string.Join(" ", semanticInfo.PublicSemanticTypes));
                    foreach (KeyValuePair<string, IList<string>> kvp in semanticInfo.SemanticProperties)
                    {
                        Log.Debug("\tRegistered property '{0}' as semantic property '{1}'", kvp.Key, string.Join(" ", kvp.Value));
                    }
                }

                return semanticInfo;
            }
        }
*/
        private SemanticInfo getSemanticInfo(Class<? extends ViewModel> modelType) throws DxaException {
            SemanticInfo semanticInfo = null;
            if (!modelTypeToSemanticInfoMapping.containsKey(modelType))
            {
                // Just-In-Time model type registration.
                semanticInfo = registerModelType(modelType);
            }
            return semanticInfo;
        }
/*
        private static SemanticInfo ExtractSemanticInfo(Type modelType)
        {
            SemanticInfo semanticInfo = new SemanticInfo();

            // Built-in semantic type mapping
            string bareTypeName = modelType.Name.Split('`')[0]; // Type name without generic type parameters (if any)
            semanticInfo.MappedSemanticTypes.Add(SemanticMapping.GetQualifiedTypeName(bareTypeName));

            // Extract semantic info from SemanticEntity attributes on the Model Type.
            foreach (SemanticEntityAttribute attribute in modelType.GetCustomAttributes(true).Where(a => a is SemanticEntityAttribute))
            {
                semanticInfo.MappedSemanticTypes.Add(SemanticMapping.GetQualifiedTypeName(attribute.EntityName, attribute.Vocab));

                if (!attribute.Public || string.IsNullOrEmpty(attribute.Prefix))
                    continue;

                string prefix = attribute.Prefix;
                string registeredVocab;
                if (semanticInfo.PrefixMappings.TryGetValue(prefix, out registeredVocab))
                {
                    // Prefix mapping already exists; must match.
                    if (attribute.Vocab != registeredVocab)
                    {
                        throw new DxaException(
                            string.Format("Attempt to use semantic prefix '{0}' for vocabulary '{1}', but is is already used for vocabulary '{2}",
                                prefix, attribute.Vocab, registeredVocab)
                            );
                    }
                }
                else
                {
                    semanticInfo.PrefixMappings.Add(prefix, attribute.Vocab);
                }

                semanticInfo.PublicSemanticTypes.Add(String.Format("{0}:{1}", prefix, attribute.EntityName));
            }

            // Extract semantic info from SemanticEntity attributes on the Model Type's properties
            foreach (MemberInfo memberInfo in modelType.GetMembers(BindingFlags.Public | BindingFlags.Instance))
            {
                foreach (SemanticPropertyAttribute attribute in memberInfo.GetCustomAttributes(true).Where(a => a is SemanticPropertyAttribute))
                {
                    if (string.IsNullOrEmpty(attribute.PropertyName))
                    {
                        // Skip properties without name.
                        continue;
                    }
                    string[] semanticPropertyNameParts = attribute.PropertyName.Split(':');
                    if (semanticPropertyNameParts.Length < 2)
                    {
                        // Skip property names without prefix.
                        continue;
                    }
                    string prefix = semanticPropertyNameParts[0];
                    if (!semanticInfo.PrefixMappings.ContainsKey(prefix))
                    {
                        // Skip property names with prefix which is not declared as public prefix on the type.
                        continue;
                    }

                    IList<string> semanticPropertyNames;
                    if (!semanticInfo.SemanticProperties.TryGetValue(memberInfo.Name, out semanticPropertyNames))
                    {
                        semanticPropertyNames = new List<string>();
                        semanticInfo.SemanticProperties.Add(memberInfo.Name, semanticPropertyNames);
                    }
                    semanticPropertyNames.Add(attribute.PropertyName);
                }
            }

            return semanticInfo;
        }

    }

     */


//    private static final Map<String, Class<? extends AbstractEntityModel>> CORE_VIEW_ENTITY_CLASS_MAP =
//            ImmutableMap.<String, Class<? extends AbstractEntityModel>>builder()
//                    .put("Accordion", ItemList.class)
//                    .put("Article", Article.class)
//                    .put("Breadcrumb", NavigationLinks.class)
//                    .put("Carousel", ItemList.class)
//                    .put("CookieNotificationBar", Notification.class)
//                    .put("Download", Download.class)
//                    .put("FooterLinkGroup", LinkList.class)
//                    .put("FooterLinks", LinkList.class)
//                    .put("HeaderLinks", LinkList.class)
//                    .put("HeaderLogo", Teaser.class)
//                    .put("Image", Image.class)
//                    .put("LeftNavigation", NavigationLinks.class)
//                    .put("LanguageSelector", Configuration.class)
//                    .put("List", ContentList.class)
//                    .put("OldBrowserNotificationBar", Notification.class)
//                    .put("PagedList", ContentList.class)
//                    .put("Place", Place.class)
//                    .put("SiteMap", SitemapItem.class)
//                    .put("SiteMapXml", SitemapItem.class)
//                    .put("SocialLinks", TagLinkList.class)
//                    .put("SocialSharing", TagLinkList.class)
//                    .put("Tab", ItemList.class)
//                    .put("Teaser-ImageOverlay", Teaser.class)
//                    .put("Teaser", Teaser.class)
//                    .put("TeaserColored", Teaser.class)
//                    .put("TeaserHero-ImageOverlay", Teaser.class)
//                    .put("TeaserMap", Teaser.class)
//                    .put("ThumbnailList", ContentList.class)
//                    .put("TopNavigation", NavigationLinks.class)
//                    .put("YouTubeVideo", YouTubeVideo.class)
//                    .build();
//
//    private final Map<String, Class<? extends AbstractEntityModel>> viewEntityClassMap = new HashMap<>();
//
//    public ViewModelRegistryImpl() {
//        viewEntityClassMap.putAll(CORE_VIEW_ENTITY_CLASS_MAP);
//    }
//
//    @Override
//     public void registerViewEntityClass(String viewName, Class<? extends AbstractEntityModel> entityClass) {
//        viewEntityClassMap.put(viewName, entityClass);
//    }
//
//    @Override
//    public Class<? extends AbstractEntityModel> getViewEntityClass(String viewName) {
//        return viewEntityClassMap.get(viewName);
//    }
//
//    @Override
//    public Class<? extends AbstractEntityModel> getMappedModelTypes(String semanticTypeName)
//    {
//    	for(String key : CORE_VIEW_ENTITY_CLASS_MAP.keySet())
//    	{
//    		if(key.equalsIgnoreCase(semanticTypeName)){
//	    		Class<? extends AbstractEntityModel> modelclass = CORE_VIEW_ENTITY_CLASS_MAP.get(key);
//	    		return modelclass;
//    		}
//    	}
//
//    	return null;
//    }
//
//    @Override
//    public Class<? extends AbstractEntityModel> getViewModelType(MvcData regionMvcData) {
//        return null;
//    }
//
//    @Override
//    public void registerViewModel(MvcData viewData, Class<? extends AbstractEntityModel> entityClass) {
//
//    }
///*
//    @Override
//    public Class<? extends AbstractEntityModel> getViewModelType(final MvcData regionMvcData) {
//
//        MvcData bareMvcData = new MvcData() {
//            @Override
//            public String getControllerAreaName() {
//                return null;
//            }
//
//            @Override
//            public String getControllerName() {
//                return return regionMvcData.getControllerName();
//            }
//
//            @Override
//            public String getActionName() {
//                return null;
//            }
//
//            @Override
//            public String getAreaName() {
//                return regionMvcData.getAreaName();
//            }
//
//            @Override
//            public String getViewName() {
//                return return regionMvcData.getViewName();
//            }
//
//            @Override
//            public String getRegionAreaName() {
//                return null;
//            }
//
//            @Override
//            public String getRegionName() {
//                return null;
//            }
//
//            @Override
//            public Map<String, String> getRouteValues() {
//                return null;
//            }
//
//            @Override
//            public Map<String, Object> getMetadata() {
//                return null;
//            }
//        }
//
//
//
//
//
//        MvcData bareMvcData = new MvcData
//        {
//            AreaName = viewData.AreaName,
//                    ControllerName = viewData.ControllerName,
//                    ViewName = viewData.ViewName
//        };
//        if (!_viewToModelTypeMapping.TryGetValue(bareMvcData, out modelType))
//        {
//            throw new DxaException(
//                    string.Format("No View Model registered for View '{0}'. Check that you have registered this View in the '{1}' area registration.", viewData, viewData.AreaName)
//            );
//        }
//        return modelType;
//
//    }*/

}
