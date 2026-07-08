import { useEffect, useRef, useState } from 'react'
import { BrowserMultiFormatReader } from '@zxing/library'

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
    let zxingReader: BrowserMultiFormatReader | null = null
    let raf: number
    const { BarcodeDetector: NativeDetector } = window as any
    const hasNative = typeof NativeDetector === 'function'

    async function start() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
        if (cancelled) { stream.getTracks().forEach(t => t.stop()); return }
        streamRef.current = stream
        if (videoRef.current) videoRef.current.srcObject = stream
      } catch (err) {
        if (cancelled) return
        const isDenied = err instanceof DOMException && err.name === 'NotAllowedError'
        setCameraError(isDenied
          ? 'Camera permission denied. Enable camera access in browser settings, or use manual entry below.'
          : 'Camera unavailable. Use manual entry below.')
        if (onError) onError(err as Error)
        return
      }

      if (hasNative) startNative()
      else startZxing()
    }

    function startNative() {
      let detector: any
      try {
        detector = new NativeDetector({ formats: ['ean_13', 'ean_8', 'code_128', 'code_39', 'upc_a', 'upc_e', 'qr_code'] })
      } catch { return startZxing() }

      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')!
      let last = 0

      function poll() {
        if (cancelled || scannedRef.current) return
        raf = requestAnimationFrame(poll)
        const video = videoRef.current
        if (!video || video.readyState < 2) return
        const now = Date.now()
        if (now - last < 500) return
        last = now
        canvas.width = video.videoWidth
        canvas.height = video.videoHeight
        ctx.drawImage(video, 0, 0)
        detector.detect(canvas).then((results: any[]) => {
          if (cancelled || scannedRef.current || !results.length) return
          scannedRef.current = true
          setDetected(true)
          onScan(results[0].rawValue)
        }).catch(() => {})
      }
      raf = requestAnimationFrame(poll)
    }

    function startZxing() {
      zxingReader = new BrowserMultiFormatReader()
      const wait = setInterval(() => {
        if (cancelled || scannedRef.current) { clearInterval(wait); return }
        const video = videoRef.current
        if (!video || video.readyState < 2) return
        clearInterval(wait)
        zxingReader!.decodeFromVideoElement(video, (result, err) => {
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
      cancelAnimationFrame(raf)
      if (zxingReader) zxingReader.reset()
      if (streamRef.current) streamRef.current.getTracks().forEach(t => t.stop())
    }
  }, [onScan, onError])

  return (
    <div className="viewfinder">
      {cameraError ? (
        <div className="viewfinder-error">
          <p>{cameraError}</p>
        </div>
      ) : (
        <video ref={videoRef} autoPlay playsInline className="viewfinder-video" />
      )}
      {detected && <div className="viewfinder-overlay" />}
    </div>
  )
}