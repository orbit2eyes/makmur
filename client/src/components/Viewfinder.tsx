import { useEffect, useRef, useState } from 'react'
import ManualEntry from './ManualEntry'

const TIMEOUT_MS = 10_000

const CAMERA_ERRORS: Record<string, string> = {
  NotAllowedError:
    'Camera permission denied. Enable camera access in browser settings, or use manual entry below.',
  NotFoundError:
    'No camera found on this device. Use manual entry below to enter the barcode.',
  NotReadableError:
    'Camera is in use by another application or browser tab. Close the other tab and try again.',
  SecurityError:
    'Camera requires a secure connection (HTTPS). Contact your IT team to enable HTTPS on this site.',
  TIMEOUT:
    'Camera stream is taking too long to start. Try reloading the page or use manual entry below.',
  UNKNOWN:
    'Camera unavailable. Use manual entry below.',
}

interface ViewfinderProps {
  onScan: (barcode: string) => void
  onError?: (err: Error) => void
}

export default function Viewfinder({ onScan, onError }: ViewfinderProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const scannedRef = useRef(false)
  const [detected, setDetected] = useState(false)
  const [cameraError, setCameraError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    let zxingReader: any = null
    let raf: number
    let timeoutId: ReturnType<typeof setTimeout> | null = null
    const { BarcodeDetector: NativeDetector } = window as any
    const hasNative = typeof NativeDetector === 'function'

    function stopStream() {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach(t => t.stop())
        streamRef.current = null
      }
    }

    function showError(err: DOMException) {
      setCameraError(CAMERA_ERRORS[err.name] || CAMERA_ERRORS.UNKNOWN)
      if (onError) onError(err)
    }

    async function getStream(): Promise<MediaStream> {
      try {
        return await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'environment' },
        })
      } catch (err) {
        if ((err as DOMException).name === 'OverconstrainedError') {
          // T-camera-01: fallback to any available camera
          return await navigator.mediaDevices.getUserMedia({ video: true })
        }
        throw err
      }
    }

    // T-camera-04: video readyState timeout
    function setReadyTimeout() {
      timeoutId = setTimeout(() => {
        if (cancelled) return
        stopStream()
        setCameraError(CAMERA_ERRORS.TIMEOUT)
        if (onError) onError(new Error('Camera stream timeout'))
      }, TIMEOUT_MS)
    }

    async function start() {
      try {
        const stream = await getStream()
        if (cancelled) { stream.getTracks().forEach(t => t.stop()); return }
        streamRef.current = stream
        if (videoRef.current) videoRef.current.srcObject = stream
      } catch (err) {
        if (cancelled) return
        showError(err as DOMException)
        return
      }

      setReadyTimeout()

      if (hasNative) startNative()
      else startZxing()
    }

    function startNative() {
      let detector: any
      try {
        detector = new NativeDetector({
          formats: ['ean_13', 'ean_8', 'code_128', 'code_39', 'upc_a', 'upc_e', 'qr_code'],
        })
      } catch {
        startZxing()
        return
      }

      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')!
      let last = 0

      function poll() {
        if (cancelled || scannedRef.current) return
        const video = videoRef.current
        if (!video || video.readyState < 2) {
          raf = requestAnimationFrame(poll)
          return
        }
        if (timeoutId) { clearTimeout(timeoutId); timeoutId = null }

        raf = requestAnimationFrame(poll)
        const now = Date.now()
        if (now - last < 500) return
        last = now
        canvas.width = video.videoWidth
        canvas.height = video.videoHeight
        ctx.drawImage(video, 0, 0)
        detector
          .detect(canvas)
          .then((results: any[]) => {
            if (cancelled || scannedRef.current || !results.length) return
            scannedRef.current = true
            setDetected(true)
            onScan(results[0].rawValue)
          })
          .catch(() => {})
      }
      raf = requestAnimationFrame(poll)
    }

    // T-camera-03: zxing import .catch() -> manual entry fallback
    async function startZxing() {
      let BrowserMultiFormatReader: any
      try {
        const mod = await import('@zxing/library')
        BrowserMultiFormatReader = mod.BrowserMultiFormatReader
      } catch {
        if (cancelled) return
        if (timeoutId) { clearTimeout(timeoutId); timeoutId = null }
        stopStream()
        setCameraError(CAMERA_ERRORS.UNKNOWN)
        if (onError) onError(new Error('Barcode decoder library failed to load'))
        return
      }
      zxingReader = new BrowserMultiFormatReader()

      const wait = setInterval(() => {
        if (cancelled || scannedRef.current) {
          clearInterval(wait)
          return
        }
        const video = videoRef.current
        if (!video || video.readyState < 2) return
        clearInterval(wait)
        if (timeoutId) { clearTimeout(timeoutId); timeoutId = null }
        zxingReader!.decodeFromVideoElement(video, (result: any, err: any) => {
          if (cancelled || scannedRef.current || !result) return
          scannedRef.current = true
          setDetected(true)
          onScan(result.getText())
        })
      }, 200)
    }

    start()

    return () => {
      cancelled = true
      if (timeoutId) clearTimeout(timeoutId)
      cancelAnimationFrame(raf)
      if (zxingReader) zxingReader.reset()
      stopStream()
    }
  }, [onScan, onError])

  return (
    <div className="viewfinder">
      {cameraError ? (
        <div className="viewfinder-error">
          <p>{cameraError}</p>
          <ManualEntry onLookup={(product) => onScan(product.barcode)} />
        </div>
      ) : (
        <video ref={videoRef} autoPlay playsInline className="viewfinder-video" />
      )}
      {detected && <div className="viewfinder-overlay" />}
    </div>
  )
}
