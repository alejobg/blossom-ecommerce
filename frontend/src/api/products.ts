import client from './client'
import type { PagedResponse } from '../types/pagination'
import type { Product, ProductSearchParams } from '../types/product'

export async function searchProducts(
  params: ProductSearchParams,
): Promise<PagedResponse<Product>> {
  const response = await client.get<PagedResponse<Product>>('/products', { params })
  return response.data
}

export async function getProductById(id: string): Promise<Product> {
  const response = await client.get<Product>(`/products/${encodeURIComponent(id)}`)
  return response.data
}
