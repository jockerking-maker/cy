import React, { useRef, useCallback } from 'react';

const exec = (command, value = null) => {
  document.execCommand(command, false, value);
};

export default function RichTextEditor({ value = '', onChange }) {
  const editorRef = useRef(null);

  const handleInput = useCallback(() => {
    if (editorRef.current && onChange) {
      onChange(editorRef.current.innerHTML);
    }
  }, [onChange]);

  const handleToolbar = useCallback((command, value = null) => {
    exec(command, value);
    handleInput();
    if (editorRef.current) {
      editorRef.current.focus();
    }
  }, [handleInput]);

  const handleImageUpload = useCallback(() => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = (e) => {
      const file = e.target.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = (ev) => {
        exec('insertImage', ev.target.result);
        handleInput();
      };
      reader.readAsDataURL(file);
    };
    input.click();
  }, [handleInput]);

  const handleLink = useCallback(() => {
    const url = prompt('请输入链接地址:', 'https://');
    if (url) {
      exec('createLink', url);
      handleInput();
    }
  }, [handleInput]);

  return (
    <div className="rich-editor">
      <div className="rich-editor-toolbar">
        <button type="button" onClick={() => handleToolbar('bold')} title="加粗">
          <strong>B</strong>
        </button>
        <button type="button" onClick={() => handleToolbar('italic')} title="斜体">
          <em>I</em>
        </button>
        <button type="button" onClick={() => handleToolbar('underline')} title="下划线">
          <u>U</u>
        </button>
        <div className="rich-editor-toolbar-divider" />
        <button type="button" onClick={() => handleToolbar('formatBlock', 'h2')} title="标题">
          H
        </button>
        <button type="button" onClick={() => handleToolbar('formatBlock', 'p')} title="段落">
          P
        </button>
        <div className="rich-editor-toolbar-divider" />
        <button type="button" onClick={() => handleToolbar('insertUnorderedList')} title="无序列表">
          \u2022
        </button>
        <button type="button" onClick={() => handleToolbar('insertOrderedList')} title="有序列表">
          1.
        </button>
        <div className="rich-editor-toolbar-divider" />
        <button type="button" onClick={handleLink} title="插入链接">
          \uD83D\uDD17
        </button>
        <button type="button" onClick={handleImageUpload} title="插入图片">
          \uD83D\uDDBC
        </button>
        <div className="rich-editor-toolbar-divider" />
        <button type="button" onClick={() => handleToolbar('removeFormat')} title="清除格式">
          清除
        </button>
      </div>
      <div
        ref={editorRef}
        className="rich-editor-content"
        contentEditable
        suppressContentEditableWarning
        onInput={handleInput}
        dangerouslySetInnerHTML={{ __html: value || '' }}
      />
    </div>
  );
}
