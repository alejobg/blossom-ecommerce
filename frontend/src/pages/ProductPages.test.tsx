import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes, useSearchParams } from 'react-router-dom'
import { getProductById, searchProducts } from '../api/products'
import type { PagedResponse, Product } from '../types/product'
import { ProductDetailPage } from './ProductDetailPage'
import { ProductListPage } from './ProductListPage'
import { CartProvider } from '../context/CartContext'

vi.mock('../api/products', () => ({
  getProductById: vi.fn(),
  searchProducts: vi.fn(),
}))

const product: Product = {
  id: 12,
  name: 'Ceramic mug',
  description: 'A handmade ceramic mug for your favorite drink.',
  price: 18.5,
  stock: 6,
  category: 'HOME',
}

function page(
  content: Product[] = [],
  currentPage = 0,
  totalPages = 0,
): PagedResponse<Product> {
  return {
    content,
    page: currentPage,
    size: 20,
    totalElements: content.length,
    totalPages,
  }
}

function QueryProbe() {
  const [params] = useSearchParams()
  return <output data-testid="url-query">{params.toString()}</output>
}

function renderProductList(path = '/products') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <ProductListPage />
      <QueryProbe />
    </MemoryRouter>,
  )
}

function renderProductDetail(path = '/products/12') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <CartProvider>
        <Routes>
          <Route element={<ProductDetailPage />} path="/products/:id" />
        </Routes>
      </CartProvider>
    </MemoryRouter>,
  )
}

function currentQuery(): URLSearchParams {
  return new URLSearchParams(screen.getByTestId('url-query').textContent ?? '')
}

describe('product catalog pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(searchProducts).mockResolvedValue(page())
    vi.mocked(getProductById).mockResolvedValue(product)
  })

  it('hydrates shareable filters from the URL and sends them to the API', async () => {
    vi.mocked(searchProducts).mockResolvedValue(page([], 2, 3))
    renderProductList('/products?name=tea&category=BOOKS&minPrice=12.50&maxPrice=30&page=2')

    expect(screen.getByLabelText('Name')).toHaveValue('tea')
    expect(screen.getByLabelText('Category')).toHaveValue('BOOKS')
    expect(screen.getByLabelText('Minimum price')).toHaveValue(12.5)
    expect(screen.getByLabelText('Maximum price')).toHaveValue(30)
    await waitFor(() => {
      expect(searchProducts).toHaveBeenLastCalledWith({
        name: 'tea',
        category: 'BOOKS',
        minPrice: 12.5,
        maxPrice: 30,
        page: 2,
        size: 20,
      })
    })
  })

  it('debounces the name filter and immediately syncs other filters to the URL', async () => {
    const user = userEvent.setup()
    vi.mocked(searchProducts).mockResolvedValue(page([], 0, 3))
    renderProductList('/products?page=2')

    await user.type(screen.getByLabelText('Name'), 'mug')
    await waitFor(() => expect(currentQuery().get('name')).toBe('mug'))
    await user.selectOptions(screen.getByLabelText('Category'), 'BOOKS')
    fireEvent.change(screen.getByLabelText('Minimum price'), { target: { value: '12.5' } })
    fireEvent.change(screen.getByLabelText('Maximum price'), { target: { value: '50' } })

    await waitFor(() => {
      const query = currentQuery()
      expect(query.get('category')).toBe('BOOKS')
      expect(query.get('minPrice')).toBe('12.5')
      expect(query.get('maxPrice')).toBe('50')
      expect(query.has('page')).toBe(false)
    })
    await waitFor(() => {
      expect(searchProducts).toHaveBeenLastCalledWith({
        name: 'mug',
        category: 'BOOKS',
        minPrice: 12.5,
        maxPrice: 50,
        page: 0,
        size: 20,
      })
    })
  })

  it('shows a loading state while the product list request is pending', () => {
    vi.mocked(searchProducts).mockImplementation(() => new Promise(() => undefined))
    renderProductList()

    expect(screen.getByText('Loading products…')).toBeInTheDocument()
  })

  it('shows an error state when the product list request fails', async () => {
    vi.mocked(searchProducts).mockRejectedValue(new Error('Request failed'))
    renderProductList()

    expect(await screen.findByRole('alert')).toHaveTextContent(/We couldn't load the products/)
    expect(screen.getByRole('alert')).toHaveTextContent('An unexpected error occurred.')
  })

  it('shows an empty state when no products match the filters', async () => {
    renderProductList()

    expect(await screen.findByText('No products found')).toBeInTheDocument()
    expect(screen.getByText('Try changing or clearing your filters.')).toBeInTheDocument()
  })

  it('shows a loading state while a product detail request is pending', () => {
    vi.mocked(getProductById).mockImplementation(() => new Promise(() => undefined))
    renderProductDetail()

    expect(screen.getByRole('status')).toHaveTextContent('Loading product')
  })

  it('shows a clear not-found state for a 404 product detail response', async () => {
    vi.mocked(getProductById).mockRejectedValue({
      isAxiosError: true,
      response: { status: 404, data: { detail: 'Not found' } },
    })
    renderProductDetail('/products/404')

    expect(await screen.findByRole('alert')).toHaveTextContent('Product not found')
    expect(screen.getByRole('alert')).toHaveTextContent(
      'This product could not be found. It may have been removed or the link is incorrect.',
    )
  })

  it('shows other product detail errors and retries into the product detail', async () => {
    const user = userEvent.setup()
    vi.mocked(getProductById).mockRejectedValue(new Error('Request failed'))
    renderProductDetail()

    expect(await screen.findByRole('alert')).toHaveTextContent(/We couldn.t load this product/)

    vi.mocked(getProductById).mockResolvedValue(product)
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('button', { name: 'Add to cart' })).toBeEnabled()
    expect(screen.getByText('$18.50')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Add to cart' }))
    expect(screen.getByRole('status')).toHaveTextContent('Added to your cart.')
  })

  it('prevents adding more than the stock shown on the product detail', async () => {
    const user = userEvent.setup()
    vi.mocked(getProductById).mockResolvedValue({ ...product, stock: 1 })
    renderProductDetail()

    await user.click(await screen.findByRole('button', { name: 'Add to cart' }))
    expect(screen.getByRole('button', { name: 'Add to cart' })).toBeDisabled()
    expect(screen.getByText('The maximum available quantity is in your cart.')).toBeInTheDocument()
  })

  it('updates the URL when moving between product pages', async () => {
    const user = userEvent.setup()
    vi.mocked(searchProducts).mockResolvedValue(page([product], 0, 2))
    renderProductList()

    await user.click(await screen.findByRole('button', { name: 'Next' }))

    await waitFor(() => expect(currentQuery().get('page')).toBe('1'))
    await waitFor(() => {
      expect(searchProducts).toHaveBeenLastCalledWith({ page: 1, size: 20 })
    })
  })
})
