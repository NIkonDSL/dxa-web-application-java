package com.sdl.dxa.caching.wrapper;

import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.MvcModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.caching.LocalizationAwareCacheKey;
import com.sdl.dxa.caching.LocalizationAwareKeyGenerator;
import com.sdl.dxa.caching.NamedCacheProvider;
import com.sdl.dxa.caching.WebRequestContextLocalizationIdProvider;
import com.sdl.webapp.common.api.WebRequestContext;
import com.sdl.webapp.common.api.localization.Localization;
import com.sdl.webapp.common.api.model.PageModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.cache.Cache;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CacheTest {

    @Mock
    private WebRequestContext webRequestContext;

    @Mock
    private Localization localization;

    private LocalizationAwareKeyGenerator keyGenerator;

    @Before
    public void init() {
        when(localization.getId()).thenReturn("42");
        when(webRequestContext.getLocalization()).thenReturn(localization);
        keyGenerator = new LocalizationAwareKeyGenerator();
        WebRequestContextLocalizationIdProvider localizationIdProvider = new WebRequestContextLocalizationIdProvider();
        ReflectionTestUtils.setField(localizationIdProvider, "webRequestContext", webRequestContext);
        ReflectionTestUtils.setField(keyGenerator, "localizationIdProvider", localizationIdProvider);
    }

    @Test
    public void shouldReturnCache() {
        shouldReturnNeededCache(PagesCopyingCache::new, "pages");

        shouldReturnNeededCache(EntitiesCache::new, "entities");
    }

    @Test
    public void shouldDelegateKeyCalculationToConcreteCaches() {
        //given
        PagesCopyingCache pagesCopyingCache = new PagesCopyingCache();
        EntitiesCache entitiesCache = new EntitiesCache();
        pagesCopyingCache.setKeyGenerator(keyGenerator);
        pagesCopyingCache.setKeyGenerator(keyGenerator);
        entitiesCache.setKeyGenerator(keyGenerator);
        MvcModelData mvcData = new MvcModelData("a", "a", "a", "c", "v", null);
        PageModelData pageData = (PageModelData) new PageModelData("1", "tcm", null, null, null, null, null, "/url")
                .setMvcData(mvcData);
        EntityModelData entityData = new EntityModelData("2", null, null, null, null, null, "1", "2", "tcm", "/url", null, null, null);
        EntityModelData entityMvcData = (EntityModelData) new EntityModelData("3", null, null, null, null, mvcData, "1", "2", "tcm", "/url", null, null, null)
                .setMvcData(mvcData);

        //when
        Object pagesCopyingCacheKey = pagesCopyingCache.getSpecificKey(pageData);
        Object entitiesCacheKey = entitiesCache.getSpecificKey(entityMvcData);
        Object entitiesNoMvcCacheKey = entitiesCache.getSpecificKey(entityData);
        Class expectedClass = localization.getClass();
        Object entitiesExpectedClassCacheKey = entitiesCache.getSpecificKey(entityData,expectedClass);
        //then
        assertEquals(keyGenerator.generate("/url", mvcData), pagesCopyingCacheKey);
        assertEquals(keyGenerator.generate("1", "2", "3", mvcData), entitiesCacheKey);
        assertEquals(keyGenerator.generate("1", "2", "2", null), entitiesNoMvcCacheKey);
        assertNotEquals(keyGenerator.generate("1", "2", null), entitiesExpectedClassCacheKey);
    }

    private void shouldReturnNeededCache(Supplier<SimpleCacheWrapper<?, ?>> supplier, String cacheName) {
        Cache cache = mock(Cache.class);

        NamedCacheProvider cacheProvider = mock(NamedCacheProvider.class);
        //noinspection unchecked
        when(cacheProvider.getCache(eq(cacheName), any(Class.class), any(Class.class))).thenReturn(cache);

        SimpleCacheWrapper<?, ?> objectCache = supplier.get();
        objectCache.setCacheProvider(cacheProvider);

        Cache<Object, Object> actual = objectCache.getCache();
        assertSame(cache, actual);
    }
}