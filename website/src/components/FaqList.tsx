import { useState } from 'react'
import { faq } from '../content'
import { Icon } from './Icon'

export function FaqList() {
  const [openIndex, setOpenIndex] = useState<number | null>(0)

  return (
    <div className="faq-list">
      {faq.map((item, index) => {
        const isOpen = openIndex === index
        const answerId = `faq-answer-${index}`
        return (
          <div className={`faq-item ${isOpen ? 'is-open' : ''}`} key={item.question}>
            <button type="button" className="faq-question" aria-expanded={isOpen} aria-controls={answerId} onClick={() => setOpenIndex(isOpen ? null : index)}>
              <span>{item.question}</span>
              <Icon name="chevron" />
            </button>
            <div id={answerId} className="faq-answer" hidden={!isOpen}>
              <p>{item.answer}</p>
            </div>
          </div>
        )
      })}
    </div>
  )
}
