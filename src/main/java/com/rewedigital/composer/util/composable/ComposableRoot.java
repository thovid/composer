package com.rewedigital.composer.util.composable;

import java.util.Optional;

import com.spotify.apollo.Response;

/**
 * The root of a a group of {@link Composable}s. Individual {@link Composable}s can
 * be created using the root as well as merged into a root.
 *
 * @param <T>
 */
public interface ComposableRoot<T extends Composable<T>> {

    public ComposableRoot<T> composedWith(T composeable);

    public T composeableFor(final Response<?> response);

    public Class<T> composableType();

    default ComposableRoot<T> composedFrom(final Composables composables) {
        Class<T> composableType = composableType();
        Optional<ComposableRoot<T>> map = composables.get(composableType).map(r -> this.composedWith((T) r));
        return map.orElse(this);
    }

    public <P> Response<P> writtenTo(Response<P> response);

}
