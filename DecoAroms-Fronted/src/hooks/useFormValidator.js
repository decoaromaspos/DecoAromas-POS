import { useState, useEffect } from 'react';
import { checkCorreoDisponible, checkUsernameDisponible } from '../services/usuarioService';

const emailValidator = (email) => {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
};

export const useFormValidator = (initialState) => {
    const [formData, setFormData] = useState(initialState);
    const [errors, setErrors] = useState({});
    const [availability, setAvailability] = useState({
        correo: { status: 'idle', message: '' }, // idle, checking, available, unavailable
        username: { status: 'idle', message: '' },
    });

    // --- Lógica de validación en tiempo real ---
    useEffect(() => {
        const username = formData.username.trim();
        if (!username) {
            setAvailability(prev => ({ ...prev, username: { status: 'idle', message: '' } }));
            return;
        }

        setAvailability(prev => ({ ...prev, username: { status: 'checking', message: 'Verificando...' } }));
        const handler = setTimeout(async () => {
            try {
                const res = await checkUsernameDisponible(username);
                setAvailability(prev => ({ ...prev, username: { status: 'available', message: res.message } }));
            } catch (error) {
                setAvailability(prev => ({ ...prev, username: { status: 'unavailable', message: error.response?.data?.message } }));
            }
        }, 500); // Debounce

        return () => clearTimeout(handler);
    }, [formData.username]);
    
    useEffect(() => {
        const correo = formData.correo.trim();
        if (!correo || !emailValidator(correo)) {
            setAvailability(prev => ({ ...prev, correo: { status: 'idle', message: '' } }));
            return;
        }
        
        setAvailability(prev => ({ ...prev, correo: { status: 'checking', message: 'Verificando...' } }));
        const handler = setTimeout(async () => {
            try {
                const res = await checkCorreoDisponible(correo);
                setAvailability(prev => ({ ...prev, correo: { status: 'available', message: res.message } }));
            } catch (error) {
                setAvailability(prev => ({ ...prev, correo: { status: 'unavailable', message: error.response?.data?.message } }));
            }
        }, 500); // Debounce

        return () => clearTimeout(handler);
    }, [formData.correo]);


    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: null }));
        }
    };

    const validateForm = () => {
        const newErrors = {};
        // Validaciones de campos obligatorios
        for (const key in initialState) {
            if (!formData[key].trim()) {
                newErrors[key] = 'Este campo es obligatorio.';
            }
        }
        // Validaciones específicas
        if (formData.correo && !emailValidator(formData.correo)) newErrors.correo = "El formato del correo es inválido.";
        if (formData.password && formData.password.length < 6) newErrors.password = "La contraseña debe tener al menos 6 caracteres.";
        if (formData.password !== formData.confirmPassword) newErrors.confirmPassword = "Las contraseñas no coinciden.";

        // Validaciones de disponibilidad
        if (availability.username.status === 'unavailable') newErrors.username = "Este username ya está en uso.";
        if (availability.correo.status === 'unavailable') newErrors.correo = "Este correo ya está en uso.";
        
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    return { formData, errors, availability, handleChange, validateForm };
};