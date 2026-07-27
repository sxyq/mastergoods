import { createReadStream, existsSync, statSync } from 'node:fs'
import { extname, resolve } from 'node:path'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'

const repoRoot = resolve(fileURLToPath(new URL('..', import.meta.url)))
const stitchRoot = resolve(repoRoot, 'stitch_exports')

function stitchExportServer(): Plugin {
  const contentTypes: Record<string, string> = {
    '.html': 'text/html; charset=utf-8',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.tsv': 'text/tab-separated-values; charset=utf-8',
    '.md': 'text/markdown; charset=utf-8',
  }

  const handler = (req: { url?: string }, res: NodeJS.WritableStream & {
    statusCode?: number
    setHeader?: (name: string, value: string) => void
  }, next: () => void) => {
    const rawUrl = req.url ?? ''
    if (!rawUrl.startsWith('/stitch_exports/')) {
      next()
      return
    }

    const relativePath = decodeURIComponent(rawUrl.split('?')[0].replace('/stitch_exports/', ''))
    const filePath = resolve(stitchRoot, relativePath)
    if (!filePath.startsWith(stitchRoot) || !existsSync(filePath) || !statSync(filePath).isFile()) {
      next()
      return
    }

    res.statusCode = 200
    res.setHeader?.('Content-Type', contentTypes[extname(filePath)] ?? 'application/octet-stream')
    createReadStream(filePath).pipe(res)
  }

  return {
    name: 'serve-stitch-exports',
    configureServer(server) {
      server.middlewares.use(handler)
    },
    configurePreviewServer(server) {
      server.middlewares.use(handler)
    },
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const base = env.VITE_PUBLIC_BASE?.trim() || '/'

  return {
    base,
    plugins: [vue(), stitchExportServer()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
  }
})
