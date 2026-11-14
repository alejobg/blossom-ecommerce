export const PRODUCT_CATEGORIES = [
  'ELECTRONICS',
  'CLOTHING',
  'HOME',
  'BOOKS',
  'SPORTS',
  'TOYS',
  'BEAUTY',
  'FOOD',
] as const

export type ProductCategory = (typeof PRODUCT_CATEGORIES)[number]

export const PRODUCT_CATEGORY_LABELS: Record<ProductCategory, string> = {
  ELECTRONICS: 'Electronics',
  CLOTHING: 'Clothing',
  HOME: 'Home',
  BOOKS: 'Books',
  SPORTS: 'Sports',
  TOYS: 'Toys',
  BEAUTY: 'Beauty',
  FOOD: 'Food',
}

export interface Product {
  id: number
  name: string
  description: string
  price: number
  stock: number
  category: ProductCategory
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ProductSearchParams {
  name?: string
  category?: ProductCategory
  minPrice?: number
  maxPrice?: number
  page: number
  size: number
}
