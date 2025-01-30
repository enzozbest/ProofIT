import PropTypes from "prop-types";
import "@/styles/typography.css";

export function TypographyH1({ children }) {
    return <h1 className="typography-h1">{children}</h1>;
}

TypographyH1.propTypes = {
    children: PropTypes.node.isRequired,
};

export function TypographyH2({ children }) {
    return <h2 className="typography-h2">{children}</h2>;
}

TypographyH2.propTypes = {
    children: PropTypes.node.isRequired,
};

export function TypographyH3({ children }) {
    return <h3 className="typography-h3">{children}</h3>;
}

TypographyH3.propTypes = {
    children: PropTypes.node.isRequired,
};

export function TypographyH4({ children }) {
    return <h4 className="typography-h4">{children}</h4>;
}

TypographyH4.propTypes = {
    children: PropTypes.node.isRequired,
};

export function TypographyP({ children }) {
    return <p className="typography-p">{children}</p>;
}

TypographyP.propTypes = {
    children: PropTypes.node.isRequired,
};

export function TypographyBlockquote({ children }) {
    return <blockquote className="typography-blockquote">{children}</blockquote>;
}

TypographyBlockquote.propTypes = {
    children: PropTypes.node.isRequired,
};

export function TypographyList({ children }) {
    return <ul className="typography-list">{children}</ul>;
}

TypographyList.propTypes = {
    children: PropTypes.node.isRequired,
};

export function TypographyInlineCode({ children }) {
    return <code className="typography-inline-code">{children}</code>;
}

TypographyInlineCode.propTypes = {
    children: PropTypes.node.isRequired,
};

export function TypographyLead({ children }) {
    return <p className="typography-lead">{children}</p>;
}

TypographyLead.propTypes = {
    children: PropTypes.node.isRequired,
};

export function TypographyLarge({ children }) {
    return <div className="typography-large">{children}</div>;
}

TypographyLarge.propTypes = {
    children: PropTypes.node.isRequired,
};

export function TypographySmall({ children }) {
    return <small className="typography-small">{children}</small>;
}

TypographySmall.propTypes = {
    children: PropTypes.node.isRequired,
};

export function TypographyMuted({ children }) {
    return <p className="typography-muted">{children}</p>;
}

TypographyMuted.propTypes = {
    children: PropTypes.node.isRequired,
};
