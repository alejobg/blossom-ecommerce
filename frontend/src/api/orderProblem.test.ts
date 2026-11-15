import { expect, it } from 'vitest'
import { getOrderProblem } from './orderProblem'

const items = [
  { productId: 42, name: 'Mug', price: 18.5, quantity: 1 },
  { productId: 43, name: 'Tea', price: 8, quantity: 1 },
]

it('prefers the 404 productId over an ID in the detail', () => {
  const error = {
    isAxiosError: true,
    response: { status: 404, data: { detail: 'Product with id 43 was not found', productId: 42 } },
  }

  expect(getOrderProblem(error, items).message).toContain('Mug is unavailable.')
})

it.each([undefined, 0, -1, 1.5, '42', Number.MAX_SAFE_INTEGER + 1])(
  'does not infer the product from detail when productId is invalid: %s',
  (productId) => {
    const error = {
      isAxiosError: true,
      response: { status: 404, data: { detail: 'Product with id 42 was not found', productId } },
    }

    expect(getOrderProblem(error, items).message).toBe(
      'Product with id 42 was not found Please remove the unavailable product from your cart.',
    )
  },
)

it('identifies a 404 product absent from the cart by its body ID', () => {
  const error = {
    isAxiosError: true,
    response: { status: 404, data: { detail: 'Product unavailable.', productId: 99 } },
  }

  expect(getOrderProblem(error, items).message).toContain('Product #99 is unavailable.')
})
