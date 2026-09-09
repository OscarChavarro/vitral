export interface MapEntry<K, V> { readonly key: K; value: V; }
export interface Map<K, V> { size(): number; isEmpty(): boolean; get(key: K): V | undefined; put(key: K, value: V): V | undefined; }
